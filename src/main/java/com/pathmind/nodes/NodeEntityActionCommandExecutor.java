package com.pathmind.nodes;

import com.pathmind.util.BlockSelection;
import com.pathmind.util.PlayerInventoryBridge;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.lang.reflect.Method;

import org.lwjgl.glfw.GLFW;

final class NodeEntityActionCommandExecutor {
    private static final Method DO_ATTACK_METHOD = resolveDoAttackMethod();
    private static final Method SYNC_SELECTED_SLOT_METHOD = resolveSyncSelectedSlotMethod();

    private final Node owner;

    NodeEntityActionCommandExecutor(Node owner) {
        this.owner = owner;
    }
    void executeInteractCommand(java.util.concurrent.CompletableFuture<Void> future) {
        if (owner.preprocessAttachedParameter(EnumSet.of(Node.ParameterUsage.POSITION), future) == Node.ParameterHandlingResult.COMPLETE) {
            return;
        }
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client == null || client.player == null || client.gameMode == null || client.level == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        InteractionHand hand = owner.resolveHand(owner.getParameter("InteractionHand"), InteractionHand.MAIN_HAND);
        boolean preferEntity = owner.getBooleanParameter("PreferEntity", true);
        boolean preferBlock = owner.getBooleanParameter("PreferBlock", true);
        boolean fallbackToItem = owner.getBooleanParameter("FallbackToItemUse", true);
        boolean swingOnSuccess = owner.getBooleanParameter("SwingOnSuccess", true);
        boolean sneakWhileInteracting = owner.getBooleanParameter("SneakWhileInteracting", false);
        boolean restoreSneak = owner.getBooleanParameter("RestoreSneakState", true);

        boolean previousSneak = client.player.isCrouching();
        if (sneakWhileInteracting) {
            client.player.setShiftKeyDown(true);
            if (client.options != null && client.options.keyShift != null) {
                client.options.keyShift.setDown(true);
            }
        }

        Runnable restoreSneakState = () -> {
            if (sneakWhileInteracting && restoreSneak) {
                client.player.setShiftKeyDown(previousSneak);
                if (client.options != null && client.options.keyShift != null) {
                    client.options.keyShift.setDown(previousSneak);
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
            ResourceLocation identifier = ResourceLocation.tryParse(normalized);
            if (identifier == null || !BuiltInRegistries.BLOCK.containsKey(identifier)) {
                restoreSneakState.run();
                String label = requestedBlockLabel != null && !requestedBlockLabel.isEmpty() ? requestedBlockLabel : configuredBlockId;
                owner.sendNodeErrorMessage(client, "Cannot interact with \"" + label + "\": unknown block identifier.");
                future.complete(null);
                return;
            }
            targetBlock = BuiltInRegistries.BLOCK.get(identifier);
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
            ResourceLocation entityResourceLocation = ResourceLocation.tryParse(normalizedEntity);

            if (entityResourceLocation == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(entityResourceLocation)) {
                restoreSneakState.run();
                owner.sendNodeErrorMessage(client, "Cannot interact with \"" + configuredEntityId + "\": unknown entity identifier.");
                future.complete(null);
                return;
            }

            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityResourceLocation);
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
            if (targetEntity.distanceToSqr(client.player.getEyePosition(1.0f)) > Node.DEFAULT_REACH_DISTANCE_SQUARED) {
                restoreSneakState.run();
                String entityName = configuredEntityId != null
                    ? configuredEntityId.replace("minecraft:", "").replace("_", " ")
                    : String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(targetEntity.getType()))
                        .replace("minecraft:", "")
                        .replace("_", " ");
                owner.sendNodeErrorMessage(client, entityName + " is too far away to interact with.");
                future.complete(null);
                return;
            }
        }

        HitResult target = client.hitResult;
        InteractionResult result = InteractionResult.PASS;
        boolean attemptedInteraction = false;

        // If an entity parameter is specified, interact with it first
        if (targetEntity != null) {
            result = client.gameMode.interact(client.player, targetEntity, hand);
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

            BlockState state = client.level.getBlockState(targetPos);
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
                ResourceLocation stateId = BuiltInRegistries.BLOCK.getKey(targetBlock);
                if (stateId != null) {
                    owner.setParameterValueAndPropagate("Block", stateId.toString());
                }
            }

            if (targetBlock != null && !state.is(targetBlock)) {
                String name = targetBlock.getName().getString();
                restoreSneakState.run();
                owner.sendNodeErrorMessage(client, name + " is not nearby for " + owner.getType().getDisplayName() + ".");
                future.complete(null);
                return;
            }

            String blockDisplayName = targetBlock.getName().getString();

            Vec3 eyePos = client.player.getEyePosition(1.0f);
            Vec3 hitVec = Vec3.atBottomCenterOf(targetPos);
            if (eyePos.distanceToSqr(hitVec) > Node.DEFAULT_REACH_DISTANCE_SQUARED) {
                restoreSneakState.run();
                owner.sendNodeErrorMessage(client, blockDisplayName + " is too far away to interact with.");
                future.complete(null);
                return;
            }

            Direction facing = Direction.getNearest(hitVec.x - eyePos.x, hitVec.y - eyePos.y, hitVec.z - eyePos.z);
            BlockHitResult manualHit = new BlockHitResult(hitVec, facing == null ? Direction.UP : facing, targetPos, false);
            target = manualHit;
            result = client.gameMode.useItemOn(client.player, hand, manualHit);
            attemptedInteraction = true;
        }

        if (!attemptedInteraction && preferEntity && target instanceof EntityHitResult entityHit) {
            result = client.gameMode.interact(client.player, entityHit.getEntity(), hand);
            attemptedInteraction = true;
        }

        if ((!attemptedInteraction || !result.consumesAction()) && preferBlock && target instanceof BlockHitResult blockHit) {
            result = client.gameMode.useItemOn(client.player, hand, blockHit);
            attemptedInteraction = true;
        }

        if ((!attemptedInteraction || (!result.consumesAction() && result != InteractionResult.PASS)) && fallbackToItem) {
            result = client.gameMode.useItem(client.player, hand);
        }

        if (swingOnSuccess && (result.consumesAction() || result == InteractionResult.PASS)) {
            client.player.swing(hand);
            if (client.player.connection != null) {
                client.player.connection.send(new ServerboundSwingPacket(hand));
            }
        }

        restoreSneakState.run();
        future.complete(null);
    }
    void executeBreakCommand(CompletableFuture<Void> future) {
        if (owner.preprocessAttachedParameter(EnumSet.of(Node.ParameterUsage.POSITION), future) == Node.ParameterHandlingResult.COMPLETE) {
            return;
        }
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null) {
            NodeExecutionCompletion.completeExceptionally(future, new RuntimeException("Minecraft client not available"));
            return;
        }

        Node parameterNode = owner.getAttachedParameter(0);
        if (parameterNode == null) {
            NodeExecutionCompletion.fail(owner, client, future, "Break requires a block or coordinate parameter.");
            return;
        }

        BlockPos targetPos = null;
        Direction breakFace = null;
        if (owner.runtimeState().runtimeParameterData != null) {
            if (owner.runtimeState().runtimeParameterData.targetBlockPos != null) {
                targetPos = owner.runtimeState().runtimeParameterData.targetBlockPos;
            } else if (owner.runtimeState().runtimeParameterData.targetVector != null) {
                Vec3 vec = owner.runtimeState().runtimeParameterData.targetVector;
                targetPos = new BlockPos(Mth.floor(vec.x), Mth.floor(vec.y), Mth.floor(vec.z));
            }
        }

        if (targetPos == null && owner.providesTrait(parameterNode, NodeValueTrait.BLOCK)) {
            List<BlockSelection> selections = owner.resolveBlocksFromParameter(parameterNode);
            if (selections.isEmpty()) {
                NodeExecutionCompletion.fail(owner, client, future, "No block selected for Break.");
                return;
            }
            Optional<BlockHitResult> currentHit = owner.getCurrentBlockHitResult();
            if (currentHit.isPresent()) {
                BlockHitResult blockHit = currentHit.get();
                BlockPos hitPos = blockHit.getBlockPos();
                if (hitPos != null) {
                    BlockState hitState = client.level.getBlockState(hitPos);
                    boolean matches = false;
                    for (BlockSelection selection : selections) {
                        if (selection.matches(hitState)) {
                            matches = true;
                            break;
                        }
                    }
                    if (matches) {
                        targetPos = hitPos;
                        breakFace = blockHit.getDirection();
                    }
                }
            }
            if (targetPos == null) {
                Optional<BlockPos> nearest = owner.findNearestBlock(client, selections, Math.sqrt(Node.DEFAULT_REACH_DISTANCE_SQUARED));
                if (nearest.isPresent()) {
                    targetPos = nearest.get();
                }
            }
        }

        if (targetPos == null) {
            NodeExecutionCompletion.fail(owner, client, future, "No matching block found in reach for Break.");
            return;
        }

        if (owner.runtimeState().runtimeParameterData == null) {
            owner.runtimeState().runtimeParameterData = new RuntimeParameterData();
        }
        owner.runtimeState().runtimeParameterData.targetBlockPos = targetPos;

        Vec3 eyePos = client.player.getEyePosition(1.0f);
        Vec3 center = Vec3.atBottomCenterOf(targetPos);
        if (eyePos.distanceToSqr(center) > Node.DEFAULT_REACH_DISTANCE_SQUARED) {
            NodeExecutionCompletion.fail(owner, client, future, "Target block is out of reach.");
            return;
        }

        if (breakFace == null) {
            Vec3 delta = center.subtract(eyePos);
            breakFace = Direction.getNearest(delta.x, delta.y, delta.z);
            if (breakFace == null) {
                breakFace = Direction.UP;
            }
        }

        BlockState state = client.level.getBlockState(targetPos);
        if (state.isAir()) {
            NodeExecutionCompletion.complete(future);
            return;
        }
        float delta = state.getDestroyProgress(client.player, client.level, targetPos);
        if (delta <= 0.0F) {
            NodeExecutionCompletion.fail(owner, client, future, "Block cannot be broken.");
            return;
        }
        int ticksToBreak = Math.max(1, (int) Math.ceil(1.0F / delta));

        Direction finalBreakFace = breakFace;
        BlockPos finalTargetPos = targetPos;
        new Thread(() -> {
            try {
                owner.runOnClientThread(client, () -> {
                    owner.orientPlayerTowardsRuntimeTarget(client, owner.runtimeState().runtimeParameterData);
                    if (client.gameMode != null) {
                        client.gameMode.startDestroyBlock(finalTargetPos, finalBreakFace);
                    }
                    client.player.swing(InteractionHand.MAIN_HAND);
                });

                for (int i = 0; i < ticksToBreak; i++) {
                    Thread.sleep(50L);
                    Boolean isAir = owner.supplyFromClient(client,
                        () -> client.level == null || client.level.getBlockState(finalTargetPos).isAir());
                    if (Boolean.TRUE.equals(isAir)) {
                        break;
                    }
                    owner.runOnClientThread(client, () -> {
                        if (client.gameMode != null) {
                            client.gameMode.continueDestroyBlock(finalTargetPos, finalBreakFace);
                        }
                    });
                }

                owner.runOnClientThread(client, () -> {
                    if (client.player != null && client.player.connection != null) {
                        client.player.connection.send(new ServerboundPlayerActionPacket(
                            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                            finalTargetPos,
                            finalBreakFace
                        ));
                    }
                });
                NodeExecutionCompletion.complete(future);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                NodeExecutionCompletion.completeExceptionally(future, e);
            }
        }, "Pathmind-Break").start();
    }
    void executeTradeCommand(CompletableFuture<Void> future) {
        if (owner.preprocessAttachedParameter(EnumSet.noneOf(Node.ParameterUsage.class), future) == Node.ParameterHandlingResult.COMPLETE) {
            return;
        }
        owner.ensureVillagerTradeNumberParameter();

        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client == null || client.player == null || client.gameMode == null) {
            NodeExecutionCompletion.completeExceptionally(future, new RuntimeException("Minecraft client not available"));
            return;
        }

        // Check if a merchant screen is open
        net.minecraft.client.gui.screens.Screen currentScreen = client.screen;
        if (!(currentScreen instanceof net.minecraft.client.gui.screens.inventory.MerchantScreen)) {
            NodeExecutionCompletion.fail(owner, client, future, "No villager trading screen is open.");
            return;
        }

        net.minecraft.client.gui.screens.inventory.MerchantScreen merchantScreen =
            (net.minecraft.client.gui.screens.inventory.MerchantScreen) currentScreen;

        // Get the screen handler from merchant screen
        net.minecraft.world.inventory.MerchantMenu screenHandler = merchantScreen.getMenu();
        if (screenHandler == null) {
            NodeExecutionCompletion.fail(owner, client, future, "Cannot access merchant screen handler.");
            return;
        }

        // Get the trade offers
        net.minecraft.world.item.trading.MerchantOffers tradeOffers = screenHandler.getOffers();
        if (tradeOffers == null || tradeOffers.isEmpty()) {
            NodeExecutionCompletion.fail(owner, client, future, "No trades available from this villager.");
            return;
        }
        int selectedTradeNumber = owner.getConfiguredVillagerTradeNumber();
        int tradeIndex = selectedTradeNumber - 1;
        if (tradeIndex < 0 || tradeIndex >= tradeOffers.size() || tradeOffers.get(tradeIndex) == null) {
            NodeExecutionCompletion.fail(owner, client, future,
                "Trade #" + selectedTradeNumber + " is not available.");
            return;
        }
        net.minecraft.world.item.trading.MerchantOffer selectedOffer = tradeOffers.get(tradeIndex);
        if (selectedOffer.isOutOfStock()) {
            NodeExecutionCompletion.fail(owner, client, future,
                "Trade #" + selectedTradeNumber + " is out of stock.");
            return;
        }
        if (!canAffordTrade(client.player, screenHandler, selectedOffer)) {
            NodeExecutionCompletion.fail(owner, client, future,
                "Not enough items for trade #" + selectedTradeNumber + ".");
            return;
        }
        List<Integer> preferredTradeIndexes = Collections.singletonList(tradeIndex);

        int tradesToExecute = owner.getConfiguredVillagerTradeCount();

        new Thread(() -> {
            try {
                int remainingTrades = tradesToExecute;
                while (remainingTrades > 0) {
                    boolean tradedThisPass = false;
                    boolean anyMatchStillAvailable = false;
                    for (Integer preferredTradeIndex : preferredTradeIndexes) {
                        if (preferredTradeIndex == null || preferredTradeIndex < 0 || preferredTradeIndex >= tradeOffers.size()) {
                            continue;
                        }
                        net.minecraft.world.item.trading.MerchantOffer offer = tradeOffers.get(preferredTradeIndex);
                        if (offer == null) {
                            continue;
                        }
                        if (!offer.isOutOfStock()) {
                            anyMatchStillAvailable = true;
                        }
                        int executableTrades = getMaxExecutableTradeCount(client.player, screenHandler, offer);
                        if (executableTrades <= 0) {
                            continue;
                        }

                        int batchSize = Math.min(remainingTrades, executableTrades);
                        selectMerchantTrade(client, screenHandler, preferredTradeIndex);
                        Thread.sleep(60);

                        int completedInBatch = 0;
                        for (int i = 0; i < batchSize; i++) {
                            if (offer.isOutOfStock() || !canAffordTrade(client.player, screenHandler, offer)) {
                                break;
                            }
                            if (!quickMoveMerchantTradeResult(client, screenHandler)) {
                                break;
                            }
                            completedInBatch++;
                            remainingTrades--;
                            tradedThisPass = true;
                            if (remainingTrades <= 0) {
                                break;
                            }
                            Thread.sleep(70);
                        }

                        if (completedInBatch > 0 && remainingTrades > 0) {
                            Thread.sleep(120);
                        }
                        if (remainingTrades <= 0) {
                            break;
                        }
                    }

                    if (!tradedThisPass) {
                        if (!anyMatchStillAvailable) {
                            owner.sendNodeErrorMessage(client, "All matching trades are out of stock.");
                        } else {
                            owner.sendNodeErrorMessage(client, "Not enough items to complete the requested trades.");
                        }
                        break;
                    }
                }

                NodeExecutionCompletion.complete(future);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                NodeExecutionCompletion.completeExceptionally(future, e);
            }
        }, "Pathmind-Trade").start();
    }

    private void selectMerchantTrade(net.minecraft.client.Minecraft client,
                                     net.minecraft.world.inventory.MerchantMenu screenHandler,
                                     int tradeIndex) throws InterruptedException {
        owner.runOnClientThread(client, () -> {
            screenHandler.setSelectionHint(tradeIndex);
            screenHandler.setSelectionHint(tradeIndex);
            if (client.player != null && client.player.connection != null) {
                client.player.connection.send(
                    new net.minecraft.network.protocol.game.ServerboundSelectTradePacket(tradeIndex)
                );
            }
        });
    }

    private boolean quickMoveMerchantTradeResult(net.minecraft.client.Minecraft client,
                                                 net.minecraft.world.inventory.MerchantMenu screenHandler) throws InterruptedException {
        if (client == null || client.player == null || client.gameMode == null || screenHandler == null) {
            return false;
        }
        final boolean[] moved = {false};
        owner.runOnClientThread(client, () -> {
            final int outputSlot = 2;
            net.minecraft.world.inventory.Slot output = screenHandler.getSlot(outputSlot);
            if (output == null) {
                return;
            }
            net.minecraft.world.item.ItemStack outputStack = output.getItem();
            if (outputStack == null || outputStack.isEmpty()) {
                return;
            }
            client.gameMode.handleInventoryMouseClick(
                screenHandler.containerId,
                outputSlot,
                0,
                net.minecraft.world.inventory.ClickType.QUICK_MOVE,
                client.player
            );
            moved[0] = true;
        });
        return moved[0];
    }

    private int getMaxExecutableTradeCount(net.minecraft.world.entity.player.Player player,
                                           net.minecraft.world.inventory.MerchantMenu screenHandler,
                                           net.minecraft.world.item.trading.MerchantOffer offer) {
        if (player == null || screenHandler == null || offer == null || offer.isOutOfStock()) {
            return 0;
        }

        int maxTrades = Integer.MAX_VALUE;
        net.minecraft.world.item.ItemStack firstBuyItem = getRequiredFirstBuyItem(offer);
        if (!firstBuyItem.isEmpty()) {
            int required = Math.max(1, firstBuyItem.getCount());
            int available = countAvailableForTrade(player.getInventory(), screenHandler, firstBuyItem);
            maxTrades = Math.min(maxTrades, available / required);
        }

        net.minecraft.world.item.ItemStack secondBuyItem = getRequiredSecondBuyItem(offer);
        if (!secondBuyItem.isEmpty()) {
            int required = Math.max(1, secondBuyItem.getCount());
            int available = countAvailableForTrade(player.getInventory(), screenHandler, secondBuyItem);
            maxTrades = Math.min(maxTrades, available / required);
        }

        maxTrades = Math.min(maxTrades, Math.max(0, offer.getMaxUses() - offer.getUses()));
        return maxTrades == Integer.MAX_VALUE ? 0 : Math.max(0, maxTrades);
    }

    boolean canAffordTrade(net.minecraft.world.entity.player.Player player,
                           net.minecraft.world.inventory.MerchantMenu screenHandler,
                           net.minecraft.world.item.trading.MerchantOffer offer) {
        if (player == null || offer == null || screenHandler == null) {
            return false;
        }

        net.minecraft.world.entity.player.Inventory inventory = player.getInventory();

        net.minecraft.world.item.ItemStack firstBuyItem = getRequiredFirstBuyItem(offer);
        if (!firstBuyItem.isEmpty()) {
            int required = firstBuyItem.getCount();
            int available = countAvailableForTrade(inventory, screenHandler, firstBuyItem);
            if (available < required) {
                return false;
            }
        }

        net.minecraft.world.item.ItemStack secondBuyItem = getRequiredSecondBuyItem(offer);
        if (!secondBuyItem.isEmpty()) {
            int required = secondBuyItem.getCount();
            int available = countAvailableForTrade(inventory, screenHandler, secondBuyItem);
            if (available < required) {
                return false;
            }
        }

        return true;
    }

    private static net.minecraft.world.item.ItemStack getRequiredFirstBuyItem(net.minecraft.world.item.trading.MerchantOffer offer) {
        return offer == null ? net.minecraft.world.item.ItemStack.EMPTY : offer.getCostA();
    }

    private static net.minecraft.world.item.ItemStack getRequiredSecondBuyItem(net.minecraft.world.item.trading.MerchantOffer offer) {
        return offer == null ? net.minecraft.world.item.ItemStack.EMPTY : offer.getCostB();
    }

    static int getRequiredFirstBuyCountForTests(net.minecraft.world.item.trading.MerchantOffer offer) {
        if (offer == null) {
            return 0;
        }
        return resolveRequiredTradeCount(
            getRequiredFirstBuyItem(offer).getCount(),
            offer.getCostA().getCount()
        );
    }

    static int getRequiredSecondBuyCountForTests(net.minecraft.world.item.trading.MerchantOffer offer) {
        if (offer == null) {
            return 0;
        }
        java.util.Optional<net.minecraft.world.item.trading.ItemCost> secondBuyItem = offer.getItemCostB();
        int originalCount = secondBuyItem.map(item -> item.count()).orElse(0);
        return resolveRequiredTradeCount(getRequiredSecondBuyItem(offer).getCount(), originalCount);
    }

    static int resolveRequiredTradeCountForTests(int displayedCount, int originalCount) {
        return resolveRequiredTradeCount(displayedCount, originalCount);
    }

    private static int resolveRequiredTradeCount(int displayedCount, int originalCount) {
        return displayedCount > 0 ? displayedCount : Math.max(0, originalCount);
    }

    private int countAvailableForTrade(net.minecraft.world.entity.player.Inventory inventory,
                                       net.minecraft.world.inventory.MerchantMenu screenHandler,
                                       net.minecraft.world.item.ItemStack requiredStack) {
        int available = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = inventory.getItem(i);
            if (net.minecraft.world.item.ItemStack.isSameItem(stack, requiredStack)) {
                available += stack.getCount();
            }
        }

        // Include items already moved into merchant input slots (0 and 1).
        for (int slotIndex = 0; slotIndex <= 1; slotIndex++) {
            net.minecraft.world.item.ItemStack stack = screenHandler.getSlot(slotIndex).getItem();
            if (net.minecraft.world.item.ItemStack.isSameItem(stack, requiredStack)) {
                available += stack.getCount();
            }
        }

        net.minecraft.world.item.ItemStack cursorStack = screenHandler.getCarried();
        if (net.minecraft.world.item.ItemStack.isSameItem(cursorStack, requiredStack)) {
            available += cursorStack.getCount();
        }

        return available;
    }
    void executeSwingCommand(CompletableFuture<Void> future) {
        if (owner.preprocessAttachedParameter(EnumSet.noneOf(Node.ParameterUsage.class), future) == Node.ParameterHandlingResult.COMPLETE) {
            return;
        }
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }

        InteractionHand hand = owner.resolveHand(owner.getParameter("InteractionHand"), InteractionHand.MAIN_HAND);
        boolean holdDurationEnabled = owner.isAmountInputEnabled();
        double durationSeconds = holdDurationEnabled
            ? Math.max(0.0, owner.getDoubleParameter("Duration", 0.0))
            : 0.0;
        int legacyCount = Math.max(1, owner.getIntParameter("Count", 1));
        double legacyIntervalSeconds = Math.max(0.0, owner.getDoubleParameter("IntervalSeconds", 0.0));

        new Thread(() -> {
            boolean releaseAttackKey = false;
            try {
                if (holdDurationEnabled && durationSeconds > 0.0) {
                    if (hand == InteractionHand.MAIN_HAND) {
                        long durationMs = (long) Math.ceil(durationSeconds * 1000.0);
                        long deadline = System.currentTimeMillis() + durationMs;
                        owner.runOnClientThread(client, () -> {
                            syncSelectedHotbarSlot(client);
                            performMainHandAttack(client);
                            if (client.options != null && client.options.keyAttack != null) {
                                client.options.keyAttack.setDown(true);
                            }
                        });
                        releaseAttackKey = true;
                        while (System.currentTimeMillis() < deadline) {
                            if (owner.shouldAbortForRepeatUntilGuard()) {
                                break;
                            }
                            long remainingMs = deadline - System.currentTimeMillis();
                            Thread.sleep(Math.min(Node.CONTROL_POLL_INTERVAL_MS, Math.max(1L, remainingMs)));
                        }
                    } else {
                        long durationMs = (long) Math.ceil(durationSeconds * 1000.0);
                        long deadline = System.currentTimeMillis() + durationMs;
                        boolean swung = false;
                        while (!swung || System.currentTimeMillis() < deadline) {
                            owner.runOnClientThread(client, () -> {
                                client.player.swing(hand);
                                if (client.player.connection != null) {
                                    client.player.connection.send(new ServerboundSwingPacket(hand));
                                }
                            });
                            swung = true;
                            if (owner.shouldAbortForRepeatUntilGuard()) {
                                break;
                            }
                            long remainingMs = deadline - System.currentTimeMillis();
                            if (remainingMs <= 0L) {
                                break;
                            }
                            Thread.sleep(Math.min(50L, remainingMs));
                        }
                    }
                } else {
                    for (int i = 0; i < legacyCount; i++) {
                        owner.runOnClientThread(client, () -> {
                            if (hand == InteractionHand.MAIN_HAND) {
                                syncSelectedHotbarSlot(client);
                                performMainHandAttack(client);
                            } else {
                                client.player.swing(hand);
                                if (client.player.connection != null) {
                                    client.player.connection.send(new ServerboundSwingPacket(hand));
                                }
                            }
                        });

                        if (legacyIntervalSeconds > 0.0 && i < legacyCount - 1) {
                            Thread.sleep((long) (legacyIntervalSeconds * 1000));
                        }
                    }
                }
                future.complete(null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.completeExceptionally(e);
            } finally {
                if (releaseAttackKey) {
                    try {
                        owner.runOnClientThread(client, () -> {
                            if (client.options != null && client.options.keyAttack != null) {
                                client.options.keyAttack.setDown(false);
                            }
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }, "Pathmind-Swing").start();
    }

    static Method resolveDoAttackMethod() {
        try {
            return net.minecraft.client.Minecraft.class.getMethod("doAttack");
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    static Method resolveSyncSelectedSlotMethod() {
        try {
            return net.minecraft.client.multiplayer.MultiPlayerGameMode.class.getMethod("syncSelectedSlot");
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    static void syncSelectedHotbarSlot(Minecraft client) {
        if (client == null) {
            return;
        }
        if (client.player != null && client.player.connection != null) {
            try {
                int selectedSlot = PlayerInventoryBridge.getSelectedSlot(client.player.getInventory());
                if (selectedSlot >= 0) {
                    client.player.connection.send(new ServerboundSetCarriedItemPacket(selectedSlot));
                }
            } catch (IllegalStateException ignored) {
                // Fall back to interaction-manager sync below.
            }
        }
        if (client.gameMode == null || SYNC_SELECTED_SLOT_METHOD == null) {
            return;
        }
        try {
            SYNC_SELECTED_SLOT_METHOD.invoke(client.gameMode);
        } catch (ReflectiveOperationException ignored) {
            // Older mappings may not expose slot sync by name.
        }
    }

    static void performMainHandAttack(Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }
        InputConstants.Key attackKey = InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        KeyMapping.click(attackKey);
        try {
            if (DO_ATTACK_METHOD != null) {
                DO_ATTACK_METHOD.invoke(client);
                return;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall back to the direct attack logic below.
        }
        if (client.gameMode != null) {
            HitResult target = client.hitResult;
            if (target instanceof EntityHitResult entityHit) {
                client.gameMode.attack(client.player, entityHit.getEntity());
                return;
            }
        }
        client.player.swing(InteractionHand.MAIN_HAND);
        if (client.player.connection != null) {
            client.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        }
    }
    void executeEquipArmorCommand(CompletableFuture<Void> future) {
        if (owner.preprocessAttachedParameter(EnumSet.noneOf(Node.ParameterUsage.class), future) == Node.ParameterHandlingResult.COMPLETE) {
            return;
        }
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        Inventory inventory = client.player.getInventory();
        int sourceSlot = owner.clampInventorySlot(inventory, owner.getIntParameter("SourceSlot", 0));
        EquipmentSlot equipmentSlot = parseEquipmentSlot(owner.getParameter("ArmorSlot"), EquipmentSlot.HEAD);
        
        ItemStack sourceStack = inventory.getItem(sourceSlot);
        if (sourceStack.isEmpty()) {
            future.complete(null);
            return;
        }
        
        ItemStack current = client.player.getItemBySlot(equipmentSlot);
        inventory.setItem(sourceSlot, current);
        client.player.setItemSlot(equipmentSlot, sourceStack);
        inventory.setChanged();
        client.player.containerMenu.broadcastChanges();
        future.complete(null);
    }
    
    void executeEquipHandCommand(CompletableFuture<Void> future) {
        if (owner.preprocessAttachedParameter(EnumSet.noneOf(Node.ParameterUsage.class), future) == Node.ParameterHandlingResult.COMPLETE) {
            return;
        }
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        Inventory inventory = client.player.getInventory();
        int sourceSlot = owner.clampInventorySlot(inventory, owner.getIntParameter("SourceSlot", 0));
        InteractionHand hand = owner.resolveHand(owner.getParameter("InteractionHand"), InteractionHand.MAIN_HAND);
        
        ItemStack sourceStack = inventory.getItem(sourceSlot);
        if (sourceStack.isEmpty()) {
            future.complete(null);
            return;
        }
        
        ItemStack handStack = client.player.getItemInHand(hand);
        client.player.setItemInHand(hand, sourceStack);
        inventory.setItem(sourceSlot, handStack);
        inventory.setChanged();
        client.player.containerMenu.broadcastChanges();
        future.complete(null);
    }

    private EquipmentSlot parseEquipmentSlot(NodeParameter parameter, EquipmentSlot defaultSlot) {
        if (parameter == null || parameter.getStringValue() == null) {
            return defaultSlot;
        }
        String value = parameter.getStringValue().trim().toLowerCase(java.util.Locale.ROOT);
        switch (value) {
            case "head":
            case "helmet":
                return EquipmentSlot.HEAD;
            case "chest":
            case "chestplate":
                return EquipmentSlot.CHEST;
            case "legs":
            case "leggings":
                return EquipmentSlot.LEGS;
            case "feet":
            case "boots":
                return EquipmentSlot.FEET;
            default:
                return defaultSlot;
        }
    }
}
