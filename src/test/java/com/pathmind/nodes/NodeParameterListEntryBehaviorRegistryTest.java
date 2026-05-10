package com.pathmind.nodes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeParameterListEntryBehaviorRegistryTest {

    @Test
    void listEntryBehaviorsAreRegisteredForRuntimeEntityFamily() {
        assertNotNull(NodeParameterListEntryBehaviorRegistry.get(NodeType.PARAM_ENTITY));
        assertNotNull(NodeParameterListEntryBehaviorRegistry.get(NodeType.PARAM_PLAYER));
        assertNotNull(NodeParameterListEntryBehaviorRegistry.get(NodeType.PARAM_ITEM));
        assertTrue(NodeParameterListEntryBehaviorRegistry.snapshot().containsKey(NodeType.PARAM_ENTITY));
    }
}
