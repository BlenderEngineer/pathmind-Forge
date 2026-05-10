package com.pathmind.nodes;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeParameterRuntimeBehaviorRegistryTest {

    @Test
    void coordinateRuntimeBehaviorIsRegistered() {
        assertNotNull(NodeParameterRuntimeBehaviorRegistry.get(NodeType.PARAM_COORDINATE));
        assertNotNull(NodeParameterRuntimeBehaviorRegistry.get(NodeType.PARAM_SCHEMATIC));
        assertNotNull(NodeParameterRuntimeBehaviorRegistry.get(NodeType.PARAM_PLACE_TARGET));
        assertNotNull(NodeParameterRuntimeBehaviorRegistry.get(NodeType.PARAM_ROTATION));
        assertNotNull(NodeParameterRuntimeBehaviorRegistry.get(NodeType.PARAM_DIRECTION));
        assertNotNull(NodeParameterRuntimeBehaviorRegistry.get(NodeType.PARAM_BLOCK_FACE));
        assertNotNull(NodeParameterRuntimeBehaviorRegistry.get(NodeType.PARAM_PLAYER));
        assertNotNull(NodeParameterRuntimeBehaviorRegistry.get(NodeType.PARAM_ENTITY));
        assertNotNull(NodeParameterRuntimeBehaviorRegistry.get(NodeType.PARAM_ITEM));
        assertNotNull(NodeParameterRuntimeBehaviorRegistry.get(NodeType.PARAM_BLOCK));
        assertNotNull(NodeParameterRuntimeBehaviorRegistry.get(NodeType.PARAM_CLOSEST));
        assertTrue(NodeParameterRuntimeBehaviorRegistry.snapshot().containsKey(NodeType.PARAM_COORDINATE));
    }

    @Test
    void coordinateRuntimeBehaviorResolvesPositionTarget() {
        Node owner = new Node(NodeType.LOOK, 0, 0);
        Node coordinate = new Node(NodeType.PARAM_COORDINATE, 0, 0);
        coordinate.getParameter("X").setStringValue("10");
        coordinate.getParameter("Y").setStringValue("64");
        coordinate.getParameter("Z").setStringValue("-3");
        RuntimeParameterData data = new RuntimeParameterData();

        Optional<Vec3d> resolved = NodeParameterRuntimeBehaviorRegistry
            .get(NodeType.PARAM_COORDINATE)
            .resolvePositionTarget(owner, coordinate, data, null);

        assertTrue(resolved.isPresent());
        assertEquals(Vec3d.ofCenter(new BlockPos(10, 64, -3)), resolved.get());
        assertEquals(new BlockPos(10, 64, -3), data.targetBlockPos);
    }

    @Test
    void schematicRuntimeBehaviorStoresSchematicName() {
        Node owner = new Node(NodeType.BUILD, 0, 0);
        Node schematic = new Node(NodeType.PARAM_SCHEMATIC, 0, 0);
        schematic.getParameter("Schematic").setStringValue("house");
        schematic.getParameter("X").setStringValue("1");
        schematic.getParameter("Y").setStringValue("2");
        schematic.getParameter("Z").setStringValue("3");
        RuntimeParameterData data = new RuntimeParameterData();

        Optional<Vec3d> resolved = NodeParameterRuntimeBehaviorRegistry
            .get(NodeType.PARAM_SCHEMATIC)
            .resolvePositionTarget(owner, schematic, data, null);

        assertTrue(resolved.isPresent());
        assertEquals(Vec3d.ofCenter(new BlockPos(1, 2, 3)), resolved.get());
        assertEquals(new BlockPos(1, 2, 3), data.targetBlockPos);
        assertEquals("house", data.schematicName);
    }

    @Test
    void placeTargetRuntimeBehaviorStoresBlockId() {
        Node owner = new Node(NodeType.PLACE, 0, 0);
        Node placeTarget = new Node(NodeType.PARAM_PLACE_TARGET, 0, 0);
        placeTarget.getParameter("Block").setStringValue("stone");
        placeTarget.getParameter("X").setStringValue("4");
        placeTarget.getParameter("Y").setStringValue("5");
        placeTarget.getParameter("Z").setStringValue("6");
        RuntimeParameterData data = new RuntimeParameterData();

        Optional<Vec3d> resolved = NodeParameterRuntimeBehaviorRegistry
            .get(NodeType.PARAM_PLACE_TARGET)
            .resolvePositionTarget(owner, placeTarget, data, null);

        assertTrue(resolved.isPresent());
        assertEquals(Vec3d.ofCenter(new BlockPos(4, 5, 6)), resolved.get());
        assertEquals(new BlockPos(4, 5, 6), data.targetBlockPos);
        assertEquals("stone", data.targetBlockId);
    }

    @Test
    void namedDirectionMappingPreservesCurrentYawForVerticalDirections() {
        NodeParameterRuntimeBehaviorRegistry.Orientation up =
            NodeParameterRuntimeBehaviorRegistry.applyDirection("up", 42.0F, 10.0F);
        NodeParameterRuntimeBehaviorRegistry.Orientation down =
            NodeParameterRuntimeBehaviorRegistry.applyDirection("down", 42.0F, 10.0F);

        assertEquals(42.0F, up.yaw);
        assertEquals(-90.0F, up.pitch);
        assertEquals(42.0F, down.yaw);
        assertEquals(90.0F, down.pitch);
    }

    @Test
    void namedDirectionMappingResolvesHorizontalDirections() {
        assertOrientation("north", 180.0F, 0.0F);
        assertOrientation("south", 0.0F, 0.0F);
        assertOrientation("west", 90.0F, 0.0F);
        assertOrientation("east", -90.0F, 0.0F);
    }

    @Test
    void unknownNamedDirectionKeepsCurrentOrientation() {
        NodeParameterRuntimeBehaviorRegistry.Orientation orientation =
            NodeParameterRuntimeBehaviorRegistry.applyDirection("sideways", 12.0F, 34.0F);

        assertEquals(12.0F, orientation.yaw);
        assertEquals(34.0F, orientation.pitch);
    }

    @Test
    void playerSearchFailureMessageUsesPlayerSelectionKind() {
        Node owner = new Node(NodeType.LOOK, 0, 0);

        assertEquals("No players nearby for pathmind.node.type.look.",
            NodeParameterRuntimeBehaviorRegistry.playerSearchFailureMessage(owner, "Any"));
        assertEquals("Local player unavailable for pathmind.node.type.look.",
            NodeParameterRuntimeBehaviorRegistry.playerSearchFailureMessage(owner, "Self"));
        assertEquals("Player \"Alex\" is not nearby for pathmind.node.type.look.",
            NodeParameterRuntimeBehaviorRegistry.playerSearchFailureMessage(owner, "Alex"));
    }

    @Test
    void noNearbyEntityMessageUsesOwnerDisplayName() {
        Node owner = new Node(NodeType.LOOK, 0, 0);

        assertEquals("No nearby entity found for pathmind.node.type.look.",
            NodeParameterRuntimeBehaviorRegistry.noNearbyEntityMessage(owner));
    }

    @Test
    void itemSearchFailureMessagesUseOwnerDisplayName() {
        Node owner = new Node(NodeType.LOOK, 0, 0);

        assertEquals("Unknown item \"missing_item\" for pathmind.node.type.look.",
            NodeParameterRuntimeBehaviorRegistry.unknownItemMessage(owner, "missing_item"));
        assertEquals("No dropped diamond, emerald found for pathmind.node.type.look.",
            NodeParameterRuntimeBehaviorRegistry.noDroppedItemMessage(owner, java.util.List.of("diamond", "emerald")));
    }

    @Test
    void blockSearchFailureMessagesUseOwnerDisplayName() {
        Node owner = new Node(NodeType.LOOK, 0, 0);

        assertEquals("No blocks defined on parameter for pathmind.node.type.look.",
            NodeParameterRuntimeBehaviorRegistry.noBlocksDefinedMessage(owner));
        assertEquals("No nearby block found for pathmind.node.type.look.",
            NodeParameterRuntimeBehaviorRegistry.noNearbyBlockMessage(owner));
        assertEquals("No matching block from parameter found for pathmind.node.type.look.",
            NodeParameterRuntimeBehaviorRegistry.noMatchingBlockMessage(owner));
        assertEquals("No open block found within range for pathmind.node.type.look.",
            NodeParameterRuntimeBehaviorRegistry.noOpenBlockMessage(owner));
    }

    private static void assertOrientation(String direction, float yaw, float pitch) {
        NodeParameterRuntimeBehaviorRegistry.Orientation orientation =
            NodeParameterRuntimeBehaviorRegistry.applyDirection(direction, 1.0F, 2.0F);
        assertEquals(yaw, orientation.yaw);
        assertEquals(pitch, orientation.pitch);
    }
}
