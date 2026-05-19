package com.pathmind.nodes;

import com.pathmind.util.EntityCompatibilityBridge;
import com.pathmind.util.EntityStateOptions;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class EntityParameterDefinition {
    static NodeBehaviorDefinition create() {
        return NodeBehaviorDefinition.builder(NodeType.PARAM_ENTITY)
            .parameterBehavior((node, values) -> NodeBehaviorDefinitionSupport.copyIfPresent(values, "Range", "Distance"))
            .runtimeBehavior(EntityParameterDefinition::resolvePositionTarget)
            .listEntryBehavior(EntityParameterDefinition::resolveListEntry)
            .gotoFallbackTargetBehavior(EntityParameterDefinition::resolveGotoFallbackTarget)
            .build();
    }

    private static Optional<Vec3> resolvePositionTarget(Node owner, Node parameterNode, RuntimeParameterData data,
                                                         CompletableFuture<Void> future) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return Optional.empty();
        }
        String state = owner.getEntityParameterState(parameterNode);
        double defaultRange = owner.getType() == NodeType.SENSOR_DISTANCE_BETWEEN ? 256.0 : Node.PARAMETER_SEARCH_RADIUS;
        double range = Node.parseNodeDouble(parameterNode, "Range", defaultRange);
        String rawEntity = Node.getParameterString(parameterNode, "Entity");

        if (Node.isAnySelectionValue(rawEntity)) {
            if (client.level == null) {
                return Optional.empty();
            }
            Optional<Entity> nearest = findNearestAnyEntity(client, range, state);
            if (nearest.isEmpty()) {
                owner.sendParameterSearchFailure(NodeBehaviorDefinitionSupport.noNearbyEntityMessage(owner), future);
                return Optional.empty();
            }
            ResourceLocation nearestResourceLocation = BuiltInRegistries.ENTITY_TYPE.getKey(nearest.get().getType());
            return resolvedEntityPosition(nearest.get(), nearestResourceLocation != null ? nearestResourceLocation.toString() : null, data);
        }

        List<String> entityIds = owner.resolveEntityIdsFromParameter(parameterNode);
        if (entityIds.isEmpty()) {
            owner.sendParameterSearchFailure("No entity selected on parameter for " + owner.getType().getDisplayName() + ".", future);
            return Optional.empty();
        }
        Entity nearest = null;
        String nearestId = null;
        double nearestDistance = Double.MAX_VALUE;
        for (String candidateId : entityIds) {
            ResourceLocation identifier = ResourceLocation.tryParse(candidateId);
            if (identifier == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)) {
                continue;
            }
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(identifier);
            Optional<Entity> entity = owner.findNearestEntity(client, entityType, range, state);
            if (entity.isEmpty()) {
                continue;
            }
            double distance = entity.get().distanceToSqr(client.player);
            if (distance < nearestDistance) {
                nearest = entity.get();
                nearestId = identifier.toString();
                nearestDistance = distance;
            }
        }
        if (nearest == null) {
            owner.sendParameterSearchFailure(NodeBehaviorDefinitionSupport.noNearbyEntityMessage(owner), future);
            return Optional.empty();
        }
        return resolvedEntityPosition(nearest, nearestId, data);
    }

    private static Node.ListValueEntry resolveListEntry(Node owner, Node parameterNode, Minecraft client) {
        String state = owner.getEntityParameterState(parameterNode);
        double range = Node.parseDoubleOrDefault(Node.getParameterString(parameterNode, "Range"), Node.PARAMETER_SEARCH_RADIUS);
        String rawEntity = Node.getParameterString(parameterNode, "Entity");
        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        if (Node.isAnySelectionValue(rawEntity)) {
            double searchRadius = Math.max(1.0, range);
            AABB searchBox = client.player.getBoundingBox().inflate(searchRadius);
            for (Entity candidate : client.level.getEntities(
                client.player,
                searchBox,
                entity -> entity != null && !entity.isRemoved() && EntityStateOptions.matchesState(entity, state))) {
                double distance = candidate.distanceToSqr(client.player);
                if (distance < nearestDistance) {
                    nearest = candidate;
                    nearestDistance = distance;
                }
            }
        } else {
            for (String candidateId : owner.resolveEntityIdsFromParameter(parameterNode)) {
                ResourceLocation identifier = ResourceLocation.tryParse(candidateId);
                if (identifier == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)) {
                    continue;
                }
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(identifier);
                Optional<Entity> candidate = owner.findNearestEntity(client, entityType, range, state);
                if (candidate.isEmpty()) {
                    continue;
                }
                double distance = candidate.get().distanceToSqr(client.player);
                if (distance < nearestDistance) {
                    nearest = candidate.get();
                    nearestDistance = distance;
                }
            }
        }
        return nearest != null ? new Node.ListValueEntry(NodeType.PARAM_ENTITY, nearest.getUUID().toString()) : null;
    }

    private static BlockPos resolveGotoFallbackTarget(Node owner, Node parameterNode, Minecraft client,
                                                      CompletableFuture<Void> future) {
        if (client == null || client.player == null || client.level == null) {
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
            ResourceLocation identifier = ResourceLocation.tryParse(candidateId);
            if (identifier == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)) {
                continue;
            }
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(identifier);
            Optional<Entity> target = owner.findNearestEntity(client, entityType, range, state);
            if (target.isEmpty()) {
                continue;
            }
            double distance = target.get().distanceToSqr(client.player);
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
            data.targetBlockPos = nearest.blockPosition();
            data.targetEntity = nearest;
        }
        return nearest.blockPosition();
    }

    private static Optional<Entity> findNearestAnyEntity(Minecraft client, double range, String state) {
        double searchRadius = Math.max(1.0, range);
        AABB searchBox = client.player.getBoundingBox().inflate(searchRadius);
        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity entity : client.level.getEntities(client.player, searchBox)) {
            if (entity == null || entity.isRemoved()) {
                continue;
            }
            if (!EntityStateOptions.matchesState(entity, state)) {
                continue;
            }
            double distance = entity.distanceToSqr(client.player);
            if (nearest == null || distance < nearestDistance) {
                nearest = entity;
                nearestDistance = distance;
            }
        }
        return Optional.ofNullable(nearest);
    }

    private static Optional<Vec3> resolvedEntityPosition(Entity entity, String entityId, RuntimeParameterData data) {
        if (data != null) {
            data.targetEntity = entity;
            data.targetEntityId = entityId;
            data.targetBlockPos = entity.blockPosition();
        }
        Vec3 entityPos = EntityCompatibilityBridge.getPos(entity);
        if (entityPos != null) {
            return Optional.of(entityPos);
        }
        return Optional.of(Vec3.atBottomCenterOf(entity.blockPosition()));
    }

    private EntityParameterDefinition() {
    }
}
