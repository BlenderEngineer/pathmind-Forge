package com.pathmind.nodes;

import com.pathmind.util.BlockSelection;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

final class NodeEntityActionCommandExecutor {
    private final Node owner;

    NodeEntityActionCommandExecutor(Node owner) {
        this.owner = owner;
    }
    void executeInteractCommand(java.util.concurrent.CompletableFuture<Void> future) {
        if (owner.preprocessAttachedParameter(EnumSet.of(Node.ParameterUsage.POSITION), future) == Node.ParameterHandlingResult.COMPLETE) {
            return;
        }
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null || client.interactionManager == null || client.world == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        Hand hand = owner.resolveHand(owner.getParameter("Hand"), Hand.MAIN_HAND);
        boolean preferEntity = owner.getBooleanParameter("PreferEntity", true);
        boolean preferBlock = owner.getBooleanParameter("PreferBlock", true);
        boolean fallbackToItem = owner.getBooleanParameter("FallbackToItemUse", true);
        boolean swingOnSuccess = owner.getBooleanParameter("SwingOnSuccess", true);
        boolean sneakWhileInteracting = owner.getBooleanParameter("SneakWhileInteracting", false);
        boolean restoreSneak = owner.getBooleanParameter("RestoreSneakState", true);

        boolean previousSneak = client.player.isSneaking();
        if (sneakWhileInteracting) {
            client.player.setSneaking(true);
            if (client.options != null && client.options.sneakKey != null) {
                client.options.sneakKey.setPressed(true);
            }
        }

        Runnable restoreSneakState = () -> {
            if (sneakWhileInteracting && restoreSneak) {
                client.player.setSneaking(previousSneak);
                if (client.options != null && client.options.sneakKey != null) {
                    client.options.sneakKey.setPressed(previousSneak);
                }
            }
        };

        RuntimeParameterData parameterData = owner.runtimeState().runtimeParameterData;
        BlockPos parameterTargetPos = parameterData != null ? parameterData.targetBlockPos : null;

        NodeParameter blockParameter = owner.getParameter("Block");
        String configuredBlockId = null;
        String requestedBlockLabel = null;
        if (parameterData != null) {
            if (parameterData.targetBlockId != null && !parameterData.targetBlockId.isEmpty()) {
                configuredBlockId = parameterData.targetBlockId;
            } else if (parameterData.targetBlockIds != null && !parameterData.targetBlockIds.isEmpty()) {
                configuredBlockId = parameterData.targetBlockIds.get(0);
            }
        }
        if (blockParameter != null) {
            String value = blockParameter.getStringValue();
            if (value != null && !value.trim().isEmpty()) {
                configuredBlockId = value.trim();
                requestedBlockLabel = value.trim();
            }
        }
        if (requestedBlockLabel == null) {
            requestedBlockLabel = configuredBlockId;
        }

        String configuredBlockSelection = configuredBlockId;

        Block targetBlock = null;
        if (configuredBlockId != null && !configuredBlockId.isEmpty()) {
            String sanitized = owner.sanitizeResourceId(configuredBlockId);
            String normalized = owner.normalizeResourceId(sanitized, "minecraft");
            Identifier identifier = Identifier.tryParse(normalized);
            if (identifier == null || !Registries.BLOCK.containsId(identifier)) {
                restoreSneakState.run();
                String label = requestedBlockLabel != null && !requestedBlockLabel.isEmpty() ? requestedBlockLabel : configuredBlockId;
                owner.sendNodeErrorMessage(client, "Cannot interact with \"" + label + "\": unknown block identifier.");
                future.complete(null);
                return;
            }
            targetBlock = Registries.BLOCK.get(identifier);
            configuredBlockId = identifier.toString();
            owner.setParameterValueAndPropagate("Block", configuredBlockId);
        }

        // Check for entity parameter
        String configuredEntityId = null;
        Entity targetEntity = null;
        if (parameterData != null) {
            if (parameterData.targetEntity != null) {
                targetEntity = parameterData.targetEntity;
            }
            if (parameterData.targetEntityId != null && !parameterData.targetEntityId.isEmpty()) {
                configuredEntityId = parameterData.targetEntityId;
            }
        }

        if (targetEntity == null && configuredEntityId != null && !configuredEntityId.isEmpty()) {
            String sanitizedEntity = owner.sanitizeResourceId(configuredEntityId);
            String normalizedEntity = owner.normalizeResourceId(sanitizedEntity, "minecraft");
            Identifier entityIdentifier = Identifier.tryParse(normalizedEntity);

            if (entityIdentifier == null || !Registries.ENTITY_TYPE.containsId(entityIdentifier)) {
                restoreSneakState.run();
                owner.sendNodeErrorMessage(client, "Cannot interact with \"" + configuredEntityId + "\": unknown entity identifier.");
                future.complete(null);
                return;
            }

            EntityType<?> entityType = Registries.ENTITY_TYPE.get(entityIdentifier);
            Optional<Entity> nearestEntity = owner.findNearestEntity(client, entityType, Node.PARAMETER_SEARCH_RADIUS);

            if (!nearestEntity.isPresent()) {
                restoreSneakState.run();
                String entityName = configuredEntityId.replace("minecraft:", "").replace("_", " ");
                owner.sendNodeErrorMessage(client, "No " + entityName + " nearby to interact with.");
                future.complete(null);
                return;
            }

            targetEntity = nearestEntity.get();
        }

        if (targetEntity != null) {
            // Check distance
            if (targetEntity.squaredDistanceTo(client.player.getEyePos()) > Node.DEFAULT_REACH_DISTANCE_SQUARED) {
                restoreSneakState.run();
                String entityName = configuredEntityId != null
                    ? configuredEntityId.replace("minecraft:", "").replace("_", " ")
                    : String.valueOf(Registries.ENTITY_TYPE.getId(targetEntity.getType()))
                        .replace("minecraft:", "")
                        .replace("_", " ");
                owner.sendNodeErrorMessage(client, entityName + " is too far away to interact with.");
                future.complete(null);
                return;
            }
        }

        HitResult target = client.crosshairTarget;
        ActionResult result = ActionResult.PASS;
        boolean attemptedInteraction = false;

        // If an entity parameter is specified, interact with it first
        if (targetEntity != null) {
            result = client.interactionManager.interactEntity(client.player, targetEntity, hand);
            attemptedInteraction = true;
        }

        if (!attemptedInteraction && (targetBlock != null || parameterTargetPos != null)) {
            BlockPos targetPos = parameterTargetPos;
            if (targetPos == null && targetBlock != null) {
                String selectionSource = configuredBlockSelection != null && !configuredBlockSelection.isEmpty()
                    ? configuredBlockSelection
                    : configuredBlockId;
                List<BlockSelection> selections = new ArrayList<>();
                if (selectionSource != null && !selectionSource.isEmpty()) {
                    BlockSelection.parse(selectionSource).ifPresent(selections::add);
                }
                Optional<BlockPos> nearest = owner.findNearestBlock(client, selections, Node.PARAMETER_SEARCH_RADIUS);
                if (nearest.isPresent()) {
                    targetPos = nearest.get();
                }
            }
            if (targetPos == null) {
                String name = targetBlock != null ? targetBlock.getName().getString()
                    : (requestedBlockLabel != null && !requestedBlockLabel.isEmpty() ? requestedBlockLabel : "block");
                restoreSneakState.run();
                owner.sendNodeErrorMessage(client, name + " is not nearby for " + owner.getType().getDisplayName() + ".");
                future.complete(null);
                return;
            }

            BlockState state = client.world.getBlockState(targetPos);
            if (state.isAir()) {
                String name = targetBlock != null ? targetBlock.getName().getString()
                    : (requestedBlockLabel != null && !requestedBlockLabel.isEmpty() ? requestedBlockLabel : "block");
                restoreSneakState.run();
                owner.sendNodeErrorMessage(client, name + " is missing for " + owner.getType().getDisplayName() + ".");
                future.complete(null);
                return;
            }

            if (targetBlock == null) {
                targetBlock = state.getBlock();
                Identifier stateId = Registries.BLOCK.getId(targetBlock);
                if (stateId != null) {
                    owner.setParameterValueAndPropagate("Block", stateId.toString());
                }
            }

            if (targetBlock != null && !state.isOf(targetBlock)) {
                String name = targetBlock.getName().getString();
                restoreSneakState.run();
                owner.sendNodeErrorMessage(client, name + " is not nearby for " + owner.getType().getDisplayName() + ".");
                future.complete(null);
                return;
            }

            String blockDisplayName = targetBlock.getName().getString();

            Vec3d eyePos = client.player.getEyePos();
            Vec3d hitVec = Vec3d.ofCenter(targetPos);
            if (eyePos.squaredDistanceTo(hitVec) > Node.DEFAULT_REACH_DISTANCE_SQUARED) {
                restoreSneakState.run();
                owner.sendNodeErrorMessage(client, blockDisplayName + " is too far away to interact with.");
                future.complete(null);
                return;
            }

            Direction facing = Direction.getFacing(hitVec.x - eyePos.x, hitVec.y - eyePos.y, hitVec.z - eyePos.z);
            BlockHitResult manualHit = new BlockHitResult(hitVec, facing == null ? Direction.UP : facing, targetPos, false);
            target = manualHit;
            result = client.interactionManager.interactBlock(client.player, hand, manualHit);
            attemptedInteraction = true;
        }

        if (!attemptedInteraction && preferEntity && target instanceof EntityHitResult entityHit) {
            result = client.interactionManager.interactEntity(client.player, entityHit.getEntity(), hand);
            attemptedInteraction = true;
        }

        if ((!attemptedInteraction || !result.isAccepted()) && preferBlock && target instanceof BlockHitResult blockHit) {
            result = client.interactionManager.interactBlock(client.player, hand, blockHit);
            attemptedInteraction = true;
        }

        if ((!attemptedInteraction || (!result.isAccepted() && result != ActionResult.PASS)) && fallbackToItem) {
            result = client.interactionManager.interactItem(client.player, hand);
        }

        if (swingOnSuccess && (result.isAccepted() || result == ActionResult.PASS)) {
            client.player.swingHand(hand);
            if (client.player.networkHandler != null) {
                client.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
            }
        }

        restoreSneakState.run();
        future.complete(null);
    }
}
