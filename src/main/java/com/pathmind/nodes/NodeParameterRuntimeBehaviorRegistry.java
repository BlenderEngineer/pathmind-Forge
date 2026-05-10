package com.pathmind.nodes;

import com.pathmind.util.EntityCompatibilityBridge;
import com.pathmind.util.GameProfileCompatibilityBridge;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import com.pathmind.util.BlockSelection;
import com.pathmind.util.EntityStateOptions;

final class NodeParameterRuntimeBehaviorRegistry {
    private static final Map<NodeType, NodeParameterRuntimeBehavior> BEHAVIORS = new EnumMap<>(NodeType.class);

    static {
        register(NodeType.PARAM_COORDINATE, (owner, parameterNode, data, future) -> {
            BlockPos pos = resolveBlockPosition(parameterNode);
            if (data != null) {
                data.targetBlockPos = pos;
            }
            return Optional.of(Vec3d.ofCenter(pos));
        });
        register(NodeType.PARAM_SCHEMATIC, (owner, parameterNode, data, future) -> {
            BlockPos pos = resolveBlockPosition(parameterNode);
            if (data != null) {
                data.targetBlockPos = pos;
                data.schematicName = Node.getParameterString(parameterNode, "Schematic");
            }
            return Optional.of(Vec3d.ofCenter(pos));
        });
        register(NodeType.PARAM_PLACE_TARGET, (owner, parameterNode, data, future) -> {
            BlockPos pos = resolveBlockPosition(parameterNode);
            if (data != null) {
                data.targetBlockPos = pos;
                data.targetBlockId = owner.getBlockParameterValue(parameterNode);
            }
            return Optional.of(Vec3d.ofCenter(pos));
        });
        register(NodeType.PARAM_ROTATION, NodeParameterRuntimeBehaviorRegistry::resolveDirectionalTarget);
        register(NodeType.PARAM_DIRECTION, NodeParameterRuntimeBehaviorRegistry::resolveDirectionalTarget);
        register(NodeType.PARAM_BLOCK_FACE, NodeParameterRuntimeBehaviorRegistry::resolveDirectionalTarget);
        register(NodeType.PARAM_PLAYER, NodeParameterRuntimeBehaviorRegistry::resolvePlayerTarget);
        register(NodeType.PARAM_ENTITY, NodeParameterRuntimeBehaviorRegistry::resolveEntityTarget);
        register(NodeType.PARAM_ITEM, NodeParameterRuntimeBehaviorRegistry::resolveItemTarget);
        register(NodeType.PARAM_BLOCK, NodeParameterRuntimeBehaviorRegistry::resolveBlockTarget);
        register(NodeType.PARAM_CLOSEST, NodeParameterRuntimeBehaviorRegistry::resolveClosestTarget);
    }

    static NodeParameterRuntimeBehavior get(NodeType type) {
        return BEHAVIORS.get(type);
    }

    static Map<NodeType, NodeParameterRuntimeBehavior> snapshot() {
        return new EnumMap<>(BEHAVIORS);
    }

    private static void register(NodeType type, NodeParameterRuntimeBehavior behavior) {
        BEHAVIORS.put(type, behavior);
    }

    private static BlockPos resolveBlockPosition(Node parameterNode) {
        int x = Node.parseNodeInt(parameterNode, "X", 0);
        int y = Node.parseNodeInt(parameterNode, "Y", 0);
        int z = Node.parseNodeInt(parameterNode, "Z", 0);
        return new BlockPos(x, y, z);
    }

    private static Optional<Vec3d> resolveDirectionalTarget(Node owner, Node parameterNode, RuntimeParameterData data,
                                                           java.util.concurrent.CompletableFuture<Void> future) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return Optional.empty();
        }
        Vec3d origin = EntityCompatibilityBridge.getPos(client.player);
        if (origin == null) {
            return Optional.empty();
        }

        NodeType parameterType = parameterNode.getType();
        Float yawParam = Node.parseNodeFloat(parameterNode, "Yaw");
        Float pitchParam = Node.parseNodeFloat(parameterNode, "Pitch");
        float yaw = yawParam != null ? yawParam : client.player.getYaw();
        float pitch = pitchParam != null ? pitchParam : client.player.getPitch();

        Orientation orientation = resolveNamedOrientation(parameterType, parameterNode, yaw, pitch);
        yaw = orientation.yaw;
        pitch = orientation.pitch;

        if (isGotoLike(owner.getType()) && !isCoordinateMode(owner.getMode())) {
            return Optional.empty();
        }

        Float yawOffset = Node.parseNodeFloat(parameterNode, "YawOffset");
        Float pitchOffset = Node.parseNodeFloat(parameterNode, "PitchOffset");
        if (yawOffset != null) {
            yaw += yawOffset;
        }
        if (pitchOffset != null) {
            pitch += pitchOffset;
        }

        double distance = Math.max(0.0, Node.parseNodeDouble(parameterNode, "Distance", defaultDistance(parameterType, parameterNode)));
        Vec3d target = projectTarget(origin, yaw, pitch, distance);
        if (data != null) {
            data.targetVector = target;
            data.targetBlockPos = new BlockPos(MathHelper.floor(target.x), MathHelper.floor(target.y), MathHelper.floor(target.z));
            data.resolvedYaw = yaw;
            data.resolvedPitch = pitch;
        }
        return Optional.of(target);
    }

    private static Optional<Vec3d> resolvePlayerTarget(Node owner, Node parameterNode, RuntimeParameterData data,
                                                       java.util.concurrent.CompletableFuture<Void> future) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            return Optional.empty();
        }
        String playerName = Node.getParameterString(parameterNode, "Player");
        Optional<AbstractClientPlayerEntity> player;
        if (Node.isAnyPlayerValue(playerName)) {
            player = Node.findNearestPlayer(client, client.player);
        } else if (Node.isSelfPlayerValue(playerName)) {
            player = Optional.of(client.player);
        } else {
            player = client.world.getPlayers().stream()
                .filter(p -> playerName.equalsIgnoreCase(GameProfileCompatibilityBridge.getName(p.getGameProfile())))
                .findFirst();
        }
        if (player.isEmpty()) {
            owner.sendParameterSearchFailure(playerSearchFailureMessage(owner, playerName), future);
            return Optional.empty();
        }
        String resolvedName = GameProfileCompatibilityBridge.getName(player.get().getGameProfile());
        if (data != null) {
            data.targetPlayerName = resolvedName != null ? resolvedName : playerName;
            data.targetEntity = player.get();
            data.targetBlockPos = player.get().getBlockPos();
        }
        Vec3d playerPos = EntityCompatibilityBridge.getPos(player.get());
        if (playerPos == null) {
            playerPos = Vec3d.ofCenter(player.get().getBlockPos());
        }
        return Optional.of(playerPos);
    }

    private static Optional<Vec3d> resolveEntityTarget(Node owner, Node parameterNode, RuntimeParameterData data,
                                                       java.util.concurrent.CompletableFuture<Void> future) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return Optional.empty();
        }
        String state = owner.getEntityParameterState(parameterNode);
        double defaultRange = owner.getType() == NodeType.SENSOR_DISTANCE_BETWEEN ? 256.0 : Node.PARAMETER_SEARCH_RADIUS;
        double range = Node.parseNodeDouble(parameterNode, "Range", defaultRange);
        String rawEntity = Node.getParameterString(parameterNode, "Entity");

        if (Node.isAnySelectionValue(rawEntity)) {
            if (client.world == null) {
                return Optional.empty();
            }
            Optional<Entity> nearest = findNearestAnyEntity(client, range, state);
            if (nearest.isEmpty()) {
                owner.sendParameterSearchFailure(noNearbyEntityMessage(owner), future);
                return Optional.empty();
            }
            Identifier nearestIdentifier = Registries.ENTITY_TYPE.getId(nearest.get().getType());
            return resolvedEntityPosition(nearest.get(), nearestIdentifier != null ? nearestIdentifier.toString() : null, data);
        }

        java.util.List<String> entityIds = owner.resolveEntityIdsFromParameter(parameterNode);
        if (entityIds.isEmpty()) {
            owner.sendParameterSearchFailure("No entity selected on parameter for " + owner.getType().getDisplayName() + ".", future);
            return Optional.empty();
        }
        Entity nearest = null;
        String nearestId = null;
        double nearestDistance = Double.MAX_VALUE;
        for (String candidateId : entityIds) {
            Identifier identifier = Identifier.tryParse(candidateId);
            if (identifier == null || !Registries.ENTITY_TYPE.containsId(identifier)) {
                continue;
            }
            EntityType<?> entityType = Registries.ENTITY_TYPE.get(identifier);
            Optional<Entity> entity = owner.findNearestEntity(client, entityType, range, state);
            if (entity.isEmpty()) {
                continue;
            }
            double distance = entity.get().squaredDistanceTo(client.player);
            if (distance < nearestDistance) {
                nearest = entity.get();
                nearestId = identifier.toString();
                nearestDistance = distance;
            }
        }
        if (nearest == null) {
            owner.sendParameterSearchFailure(noNearbyEntityMessage(owner), future);
            return Optional.empty();
        }
        return resolvedEntityPosition(nearest, nearestId, data);
    }

    private static Optional<Vec3d> resolveItemTarget(Node owner, Node parameterNode, RuntimeParameterData data,
                                                     java.util.concurrent.CompletableFuture<Void> future) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return Optional.empty();
        }
        java.util.List<String> itemIds = owner.resolveItemIdsFromParameter(parameterNode);
        if (itemIds.isEmpty()) {
            owner.sendParameterSearchFailure("No item selected on parameter for " + owner.getType().getDisplayName() + ".", future);
            return Optional.empty();
        }
        double defaultRange = owner.getType() == NodeType.SENSOR_DISTANCE_BETWEEN ? 256.0 : Node.PARAMETER_SEARCH_RADIUS;
        double range = Node.parseNodeDouble(parameterNode, "Range", defaultRange);
        boolean hasValidCandidate = false;
        for (String candidateId : itemIds) {
            Identifier identifier = Identifier.tryParse(candidateId);
            if (identifier == null || !Registries.ITEM.containsId(identifier)) {
                continue;
            }
            hasValidCandidate = true;
            Item item = Registries.ITEM.get(identifier);
            Optional<BlockPos> match = owner.findNearestDroppedItem(client, item, range);
            if (match.isEmpty()) {
                continue;
            }
            if (data != null) {
                data.targetBlockPos = match.get();
                data.targetItem = item;
                data.targetItemId = candidateId;
            }
            return Optional.of(Vec3d.ofCenter(match.get()));
        }
        if (!hasValidCandidate) {
            owner.sendParameterSearchFailure(unknownItemMessage(owner, itemIds.get(0)), future);
            return Optional.empty();
        }
        owner.sendParameterSearchFailure(noDroppedItemMessage(owner, itemIds), future);
        return Optional.empty();
    }

    private static Optional<Vec3d> resolveBlockTarget(Node owner, Node parameterNode, RuntimeParameterData data,
                                                      java.util.concurrent.CompletableFuture<Void> future) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            return Optional.empty();
        }
        String rawBlock = Node.getParameterString(parameterNode, "Block");
        List<BlockSelection> blocks = owner.resolveBlocksFromParameter(parameterNode);
        double range = Node.parseNodeDouble(parameterNode, "Range", Node.PARAMETER_SEARCH_RADIUS);
        if (blocks.isEmpty()) {
            if (!Node.isAnySelectionValue(rawBlock)) {
                owner.sendParameterSearchFailure(noBlocksDefinedMessage(owner), future);
                return Optional.empty();
            }
            Optional<BlockPos> nearest = owner.findNearestAnyBlock(client, range);
            if (nearest.isEmpty()) {
                owner.sendParameterSearchFailure(noNearbyBlockMessage(owner), future);
                return Optional.empty();
            }
            if (data != null) {
                data.targetBlockPos = nearest.get();
                data.targetBlockIds = new ArrayList<>();
            }
            return Optional.of(Vec3d.ofCenter(nearest.get()));
        }

        Optional<BlockPos> match = owner.findNearestBlock(client, blocks, range);
        if (match.isEmpty()) {
            owner.sendParameterSearchFailure(noMatchingBlockMessage(owner), future);
            return Optional.empty();
        }
        if (data != null) {
            data.targetBlockPos = match.get();
            data.targetBlockIds = new ArrayList<>();
            for (BlockSelection selection : blocks) {
                Identifier id = selection.getBlockId();
                if (id != null) {
                    data.targetBlockIds.add(selection.asString());
                }
            }
        }
        return Optional.of(Vec3d.ofCenter(match.get()));
    }

    private static Optional<Vec3d> resolveClosestTarget(Node owner, Node parameterNode, RuntimeParameterData data,
                                                        java.util.concurrent.CompletableFuture<Void> future) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            return Optional.empty();
        }
        int range = Math.max(1, Node.parseNodeInt(parameterNode, "Range", 5));
        Optional<BlockPos> open = owner.findNearestOpenBlock(client, range);
        if (open.isEmpty()) {
            owner.sendParameterSearchFailure(noOpenBlockMessage(owner), future);
            return Optional.empty();
        }
        if (data != null) {
            data.targetBlockPos = open.get();
        }
        return Optional.of(Vec3d.ofCenter(open.get()));
    }

    static String playerSearchFailureMessage(Node owner, String playerName) {
        if (Node.isAnyPlayerValue(playerName)) {
            return "No players nearby for " + owner.getType().getDisplayName() + ".";
        }
        if (Node.isSelfPlayerValue(playerName)) {
            return "Local player unavailable for " + owner.getType().getDisplayName() + ".";
        }
        return "Player \"" + playerName + "\" is not nearby for " + owner.getType().getDisplayName() + ".";
    }

    static String noNearbyEntityMessage(Node owner) {
        return "No nearby entity found for " + owner.getType().getDisplayName() + ".";
    }

    static String unknownItemMessage(Node owner, String reference) {
        return "Unknown item \"" + reference + "\" for " + owner.getType().getDisplayName() + ".";
    }

    static String noDroppedItemMessage(Node owner, java.util.List<String> itemIds) {
        return "No dropped " + String.join(", ", itemIds) + " found for " + owner.getType().getDisplayName() + ".";
    }

    static String noBlocksDefinedMessage(Node owner) {
        return "No blocks defined on parameter for " + owner.getType().getDisplayName() + ".";
    }

    static String noNearbyBlockMessage(Node owner) {
        return "No nearby block found for " + owner.getType().getDisplayName() + ".";
    }

    static String noMatchingBlockMessage(Node owner) {
        return "No matching block from parameter found for " + owner.getType().getDisplayName() + ".";
    }

    static String noOpenBlockMessage(Node owner) {
        return "No open block found within range for " + owner.getType().getDisplayName() + ".";
    }

    private static Optional<Entity> findNearestAnyEntity(MinecraftClient client, double range, String state) {
        double searchRadius = Math.max(1.0, range);
        Box searchBox = client.player.getBoundingBox().expand(searchRadius);
        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity entity : client.world.getOtherEntities(client.player, searchBox)) {
            if (entity == null || entity.isRemoved()) {
                continue;
            }
            if (!EntityStateOptions.matchesState(entity, state)) {
                continue;
            }
            double distance = entity.squaredDistanceTo(client.player);
            if (nearest == null || distance < nearestDistance) {
                nearest = entity;
                nearestDistance = distance;
            }
        }
        return Optional.ofNullable(nearest);
    }

    private static Optional<Vec3d> resolvedEntityPosition(Entity entity, String entityId, RuntimeParameterData data) {
        if (data != null) {
            data.targetEntity = entity;
            data.targetEntityId = entityId;
            data.targetBlockPos = entity.getBlockPos();
        }
        Vec3d entityPos = EntityCompatibilityBridge.getPos(entity);
        if (entityPos != null) {
            return Optional.of(entityPos);
        }
        return Optional.of(Vec3d.ofCenter(entity.getBlockPos()));
    }

    static Orientation resolveNamedOrientation(NodeType parameterType, Node parameterNode, float currentYaw, float currentPitch) {
        if (parameterType == NodeType.PARAM_DIRECTION && parameterNode.isDirectionModeCardinal()) {
            String direction = Node.getParameterString(parameterNode, "Direction");
            return applyDirection(direction, currentYaw, currentPitch);
        }
        if (parameterType == NodeType.PARAM_BLOCK_FACE) {
            String direction = Node.getParameterString(parameterNode, "Face");
            if (direction == null || direction.trim().isEmpty()) {
                direction = Node.getParameterString(parameterNode, "Side");
            }
            return applyDirection(direction, currentYaw, currentPitch);
        }
        return new Orientation(currentYaw, currentPitch);
    }

    static Orientation applyDirection(String direction, float currentYaw, float currentPitch) {
        if (direction == null) {
            return new Orientation(currentYaw, currentPitch);
        }
        switch (direction.trim().toLowerCase(Locale.ROOT)) {
            case "north":
                return new Orientation(180.0F, 0.0F);
            case "south":
                return new Orientation(0.0F, 0.0F);
            case "west":
                return new Orientation(90.0F, 0.0F);
            case "east":
                return new Orientation(-90.0F, 0.0F);
            case "up":
                return new Orientation(currentYaw, -90.0F);
            case "down":
                return new Orientation(currentYaw, 90.0F);
            default:
                return new Orientation(currentYaw, currentPitch);
        }
    }

    private static boolean isGotoLike(NodeType ownerType) {
        return ownerType == NodeType.GOTO || ownerType == NodeType.TRAVEL || ownerType == NodeType.GOAL;
    }

    private static boolean isCoordinateMode(NodeMode mode) {
        return mode == NodeMode.GOTO_XYZ
            || mode == NodeMode.GOTO_XZ
            || mode == NodeMode.GOAL_XYZ
            || mode == NodeMode.GOAL_XZ;
    }

    private static double defaultDistance(NodeType parameterType, Node parameterNode) {
        if (parameterType == NodeType.PARAM_DIRECTION) {
            return parameterNode.isDirectionModeExact() ? Node.DEFAULT_DIRECTION_DISTANCE : 1.0;
        }
        if (parameterType == NodeType.PARAM_BLOCK_FACE) {
            return 1.0;
        }
        return Node.DEFAULT_DIRECTION_DISTANCE;
    }

    private static Vec3d projectTarget(Vec3d origin, float yaw, float pitch, double distance) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double xDir = -Math.sin(yawRad) * Math.cos(pitchRad);
        double yDir = -Math.sin(pitchRad);
        double zDir = Math.cos(yawRad) * Math.cos(pitchRad);
        return origin.add(xDir * distance, yDir * distance, zDir * distance);
    }

    static final class Orientation {
        final float yaw;
        final float pitch;

        Orientation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private NodeParameterRuntimeBehaviorRegistry() {
    }
}
