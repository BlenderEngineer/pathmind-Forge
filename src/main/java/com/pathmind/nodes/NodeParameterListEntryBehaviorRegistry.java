package com.pathmind.nodes;

import com.pathmind.util.EntityStateOptions;
import com.pathmind.util.GameProfileCompatibilityBridge;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

final class NodeParameterListEntryBehaviorRegistry {
    private static final Map<NodeType, NodeParameterListEntryBehavior> BEHAVIORS = new EnumMap<>(NodeType.class);

    static {
        register(NodeType.PARAM_ENTITY, NodeParameterListEntryBehaviorRegistry::resolveEntityEntry);
        register(NodeType.PARAM_PLAYER, NodeParameterListEntryBehaviorRegistry::resolvePlayerEntry);
        register(NodeType.PARAM_ITEM, NodeParameterListEntryBehaviorRegistry::resolveItemEntry);
    }

    static NodeParameterListEntryBehavior get(NodeType type) {
        return BEHAVIORS.get(type);
    }

    static Map<NodeType, NodeParameterListEntryBehavior> snapshot() {
        return new EnumMap<>(BEHAVIORS);
    }

    private static void register(NodeType type, NodeParameterListEntryBehavior behavior) {
        BEHAVIORS.put(type, behavior);
    }

    private static Node.ListValueEntry resolveEntityEntry(Node owner, Node parameterNode, MinecraftClient client) {
        String state = owner.getEntityParameterState(parameterNode);
        double range = Node.parseDoubleOrDefault(Node.getParameterString(parameterNode, "Range"), Node.PARAMETER_SEARCH_RADIUS);
        String rawEntity = Node.getParameterString(parameterNode, "Entity");
        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        if (Node.isAnySelectionValue(rawEntity)) {
            double searchRadius = Math.max(1.0, range);
            Box searchBox = client.player.getBoundingBox().expand(searchRadius);
            for (Entity candidate : client.world.getOtherEntities(
                client.player,
                searchBox,
                entity -> entity != null && !entity.isRemoved() && EntityStateOptions.matchesState(entity, state))) {
                double distance = candidate.squaredDistanceTo(client.player);
                if (distance < nearestDistance) {
                    nearest = candidate;
                    nearestDistance = distance;
                }
            }
        } else {
            for (String candidateId : owner.resolveEntityIdsFromParameter(parameterNode)) {
                Identifier identifier = Identifier.tryParse(candidateId);
                if (identifier == null || !Registries.ENTITY_TYPE.containsId(identifier)) {
                    continue;
                }
                EntityType<?> entityType = Registries.ENTITY_TYPE.get(identifier);
                Optional<Entity> candidate = owner.findNearestEntity(client, entityType, range, state);
                if (candidate.isEmpty()) {
                    continue;
                }
                double distance = candidate.get().squaredDistanceTo(client.player);
                if (distance < nearestDistance) {
                    nearest = candidate.get();
                    nearestDistance = distance;
                }
            }
        }
        return nearest != null ? new Node.ListValueEntry(NodeType.PARAM_ENTITY, nearest.getUuidAsString()) : null;
    }

    private static Node.ListValueEntry resolvePlayerEntry(Node owner, Node parameterNode, MinecraftClient client) {
        String playerName = Node.getParameterString(parameterNode, "Player");
        if (Node.isSelfPlayerValue(playerName)) {
            return new Node.ListValueEntry(NodeType.PARAM_PLAYER, client.player.getUuidAsString());
        }
        if (Node.isAnyPlayerValue(playerName)) {
            Optional<AbstractClientPlayerEntity> nearest = Node.findNearestPlayer(client, client.player);
            return nearest.map(player -> new Node.ListValueEntry(NodeType.PARAM_PLAYER, player.getUuidAsString())).orElse(null);
        }
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player != null && playerName != null && playerName.equalsIgnoreCase(
                GameProfileCompatibilityBridge.getName(player.getGameProfile()))) {
                return new Node.ListValueEntry(NodeType.PARAM_PLAYER, player.getUuidAsString());
            }
        }
        return null;
    }

    private static Node.ListValueEntry resolveItemEntry(Node owner, Node parameterNode, MinecraftClient client) {
        double range = Node.parseDoubleOrDefault(Node.getParameterString(parameterNode, "Range"), Node.PARAMETER_SEARCH_RADIUS);
        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (String candidateId : owner.resolveItemIdsFromParameter(parameterNode)) {
            Identifier identifier = Identifier.tryParse(candidateId);
            if (identifier == null || !Registries.ITEM.containsId(identifier)) {
                continue;
            }
            Item item = Registries.ITEM.get(identifier);
            for (ItemEntity itemEntity : owner.findItemsByType(client, item, range)) {
                if (itemEntity == null || itemEntity.isRemoved()) {
                    continue;
                }
                double distance = itemEntity.squaredDistanceTo(client.player);
                if (distance < nearestDistance) {
                    nearest = itemEntity;
                    nearestDistance = distance;
                }
            }
        }
        return nearest != null ? new Node.ListValueEntry(NodeType.PARAM_ITEM, nearest.getUuidAsString()) : null;
    }

    private NodeParameterListEntryBehaviorRegistry() {
    }
}
