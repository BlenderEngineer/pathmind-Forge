package com.pathmind.nodes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeGotoFallbackTargetBehaviorRegistryTest {

    @Test
    void gotoFallbackBehaviorsAreRegisteredForTargetParameters() {
        assertNotNull(NodeGotoFallbackTargetBehaviorRegistry.get(NodeType.PARAM_ITEM));
        assertNotNull(NodeGotoFallbackTargetBehaviorRegistry.get(NodeType.PARAM_ENTITY));
        assertNotNull(NodeGotoFallbackTargetBehaviorRegistry.get(NodeType.PARAM_PLAYER));
        assertNotNull(NodeGotoFallbackTargetBehaviorRegistry.get(NodeType.PARAM_BLOCK));
        assertNotNull(NodeGotoFallbackTargetBehaviorRegistry.get(NodeType.LIST_ITEM));
        assertTrue(NodeGotoFallbackTargetBehaviorRegistry.snapshot().containsKey(NodeType.PARAM_ITEM));
    }
}
