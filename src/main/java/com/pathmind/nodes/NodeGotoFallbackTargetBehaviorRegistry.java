package com.pathmind.nodes;

import com.pathmind.util.GameProfileCompatibilityBridge;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

final class NodeGotoFallbackTargetBehaviorRegistry {
    private static final Map<NodeType, NodeGotoFallbackTargetBehavior> BEHAVIORS = new EnumMap<>(NodeType.class);

    static {
        register(NodeType.PARAM_ITEM, NodeGotoFallbackTargetBehaviorRegistry::resolveItemTarget);
        register(NodeType.PARAM_ENTITY, NodeGotoFallbackTargetBehaviorRegistry::resolveEntityTarget);
        register(NodeType.PARAM_PLAYER, NodeGotoFallbackTargetBehaviorRegistry::resolvePlayerTarget);
        register(NodeType.PARAM_BLOCK, NodeGotoFallbackTargetBehaviorRegistry::resolveBlockTarget);
        register(NodeType.LIST_ITEM, NodeGotoFallbackTargetBehaviorRegistry::resolveListItemTarget);
    }

    static NodeGotoFallbackTargetBehavior get(NodeType type) {
        return BEHAVIORS.get(type);
    }

    static Map<NodeType, NodeGotoFallbackTargetBehavior> snapshot() {
        return new EnumMap<>(BEHAVIORS);
    }

    private static void register(NodeType type, NodeGotoFallbackTargetBehavior behavior) {
        BEHAVIORS.put(type, behavior);
    }

    private static BlockPos resolveItemTarget(Node owner, Node parameterNode, MinecraftClient client, CompletableFuture<Void> future) {
        if (client == null || client.player == null || client.world == null) {
            return null;
        }
        List<String> itemIds = owner.resolveItemIdsFromParameter(parameterNode);
        if (itemIds.isEmpty()) {
            owner.sendNodeErrorMessage(client, "No item selected for " + owner.getType().getDisplayName() + ".");
            future.complete(null);
            return null;
        }
        double searchRange = Node.parseDoubleOrDefault(Node.getParameterString(parameterNode, "Range"), Node.PARAMETER_SEARCH_RADIUS);
        Optional<BlockPos> matchedPosition = Optional.empty();
        Item matchedItem = null;
        String matchedItemId = null;

        for (String candidateId : itemIds) {
            Identifier identifier = Identifier.tryParse(candidateId);
            if (identifier == null || !Registries.ITEM.containsId(identifier)) {
                continue;
            }
            Item candidateItem = Registries.ITEM.get(identifier);
            Optional<BlockPos> target = owner.findNearestDroppedItem(client, candidateItem, searchRange);
            if (target.isPresent()) {
                matchedPosition = target;
                matchedItem = candidateItem;
                matchedItemId = candidateId;
                break;
            }
        }

        if (matchedPosition.isEmpty()) {
            String reference = String.join(", ", itemIds);
            owner.sendNodeErrorMessage(client, "No dropped " + reference + " found nearby for " + owner.getType().getDisplayName() + ".");
            future.complete(null);
            return null;
        }

        RuntimeParameterData data = owner.getRuntimeState().runtimeParameterData;
        if (data != null) {
            data.targetBlockPos = matchedPosition.get();
            data.targetItem = matchedItem;
            data.targetItemId = matchedItemId;
        }

        return matchedPosition.get();
    }

    private static BlockPos resolveEntityTarget(Node owner, Node parameterNode, MinecraftClient client, CompletableFuture<Void> future) {
        if (client == null || client.player == null || client.world == null) {
            return null;
        }
        List<String> entityIds = owner.resolveEntityIdsFromParameter(parameterNode);
        if (entityIds.isEmpty()) {
            owner.sendNodeErrorMessage(client, "No entity selected for " + owner.getType().getDisplayName() + ".");
            future.complete(null);
            return null;
        }
        String state = owner.getEntityParameterState(parameterNode);
        double range = Node.parseDoubleOrDefault(Node.getParameterString(parameterNode, "Range"), Node.PARAMETER_SEARCH_RADIUS);
        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (String candidateId : entityIds) {
            Identifier identifier = Identifier.tryParse(candidateId);
            if (identifier == null || !Registries.ENTITY_TYPE.containsId(identifier)) {
                continue;
            }
            EntityType<?> entityType = Registries.ENTITY_TYPE.get(identifier);
            Optional<Entity> target = owner.findNearestEntity(client, entityType, range, state);
            if (target.isEmpty()) {
                continue;
            }
            double distance = target.get().squaredDistanceTo(client.player);
            if (distance < nearestDistance) {
                nearest = target.get();
                nearestDistance = distance;
            }
        }
        if (nearest == null) {
            owner.sendNodeErrorMessage(client, "No matching entity found nearby for " + owner.getType().getDisplayName() + ".");
            future.complete(null);
            return null;
        }
        RuntimeParameterData data = owner.getRuntimeState().runtimeParameterData;
        if (data != null) {
            data.targetBlockPos = nearest.getBlockPos();
            data.targetEntity = nearest;
        }
        return nearest.getBlockPos();
    }

    private static BlockPos resolvePlayerTarget(Node owner, Node parameterNode, MinecraftClient client, CompletableFuture<Void> future) {
        if (client == null || client.player == null || client.world == null) {
            return null;
        }
        String playerName = Node.getParameterString(parameterNode, "Player");
        if (Node.isSelfPlayerValue(playerName)) {
            future.complete(null);
            return null;
        }
        Optional<AbstractClientPlayerEntity> match;
        if (Node.isAnyPlayerValue(playerName)) {
            match = Node.findNearestPlayer(client, client.player);
        } else if (Node.isSelfPlayerValue(playerName)) {
            match = Optional.of(client.player);
        } else {
            match = client.world.getPlayers().stream()
                .filter(p -> playerName.equalsIgnoreCase(
                    GameProfileCompatibilityBridge.getName(p.getGameProfile())))
                .findFirst();
        }

        if (match.isEmpty()) {
            owner.sendNodeErrorMessage(client, NodeParameterRuntimeBehaviorRegistry.playerSearchFailureMessage(owner, playerName));
            future.complete(null);
            return null;
        }

        RuntimeParameterData data = owner.getRuntimeState().runtimeParameterData;
        if (data != null) {
            data.targetBlockPos = match.get().getBlockPos();
            data.targetEntity = match.get();
        }
        return match.get().getBlockPos();
    }

    private static BlockPos resolveBlockTarget(Node owner, Node parameterNode, MinecraftClient client, CompletableFuture<Void> future) {
        String blockId = owner.getBlockParameterValue(parameterNode);
        BlockPos pos = owner.resolveGotoFallbackTargetFromBlockId(blockId, future);
        RuntimeParameterData data = owner.getRuntimeState().runtimeParameterData;
        if (pos != null && data != null) {
            data.targetBlockPos = pos;
        }
        return pos;
    }

    private static BlockPos resolveListItemTarget(Node owner, Node parameterNode, MinecraftClient client, CompletableFuture<Void> future) {
        RuntimeParameterData data = owner.getRuntimeState().runtimeParameterData;
        Entity target = owner.resolveListItemEntity(parameterNode, data, future);
        if (target == null) {
            return null;
        }
        if (data != null) {
            data.targetBlockPos = target.getBlockPos();
            data.targetEntity = target;
        }
        return target.getBlockPos();
    }

    private NodeGotoFallbackTargetBehaviorRegistry() {
    }
}
