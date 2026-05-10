package com.pathmind.nodes;

import java.util.EnumMap;
import java.util.Map;

final class NodeBehaviorDefinitionRegistry {
    private static final Map<NodeType, NodeBehaviorDefinition> DEFINITIONS = new EnumMap<>(NodeType.class);

    static {
        for (NodeType type : NodeType.values()) {
            NodeBehaviorDefinition definition = NodeBehaviorDefinition.builder(type)
                .parameterBehavior(NodeParameterBehaviorRegistry.get(type))
                .runtimeBehavior(NodeParameterRuntimeBehaviorRegistry.get(type))
                .listEntryBehavior(NodeParameterListEntryBehaviorRegistry.get(type))
                .gotoFallbackTargetBehavior(NodeGotoFallbackTargetBehaviorRegistry.get(type))
                .comparableBehavior(NodeComparableBehaviorRegistry.get(type))
                .build();
            if (definition.hasAnyBehavior()) {
                DEFINITIONS.put(type, definition);
            }
        }
    }

    static NodeBehaviorDefinition get(NodeType type) {
        return DEFINITIONS.get(type);
    }

    static Map<NodeType, NodeBehaviorDefinition> snapshot() {
        return new EnumMap<>(DEFINITIONS);
    }

    private NodeBehaviorDefinitionRegistry() {
    }
}
