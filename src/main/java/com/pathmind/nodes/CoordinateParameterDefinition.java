package com.pathmind.nodes;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

final class CoordinateParameterDefinition {
    static NodeBehaviorDefinition create() {
        return NodeBehaviorDefinition.builder(NodeType.PARAM_COORDINATE)
            .runtimeBehavior(CoordinateParameterDefinition::resolvePositionTarget)
            .comparableBehavior(NodeBehaviorDefinitionSupport.stringComparable((owner, node) -> {
                String formatted = owner.formatCoordinateValues(node.exportParameterValues());
                return formatted.isEmpty() ? Optional.empty() : Optional.of(formatted);
            }))
            .build();
    }

    static BlockPos resolveBlockPosition(Node parameterNode) {
        int x = Node.parseNodeInt(parameterNode, "X", 0);
        int y = Node.parseNodeInt(parameterNode, "Y", 0);
        int z = Node.parseNodeInt(parameterNode, "Z", 0);
        return new BlockPos(x, y, z);
    }

    private static Optional<Vec3> resolvePositionTarget(Node owner, Node parameterNode, RuntimeParameterData data,
                                                         CompletableFuture<Void> future) {
        BlockPos pos = resolveBlockPosition(parameterNode);
        if (data != null) {
            data.targetBlockPos = pos;
        }
        return Optional.of(Vec3.atBottomCenterOf(pos));
    }

    private CoordinateParameterDefinition() {
    }
}
