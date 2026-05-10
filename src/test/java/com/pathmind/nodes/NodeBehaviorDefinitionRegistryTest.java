package com.pathmind.nodes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeBehaviorDefinitionRegistryTest {

    @Test
    void targetParameterFamilyHasConsolidatedDefinitions() {
        NodeBehaviorDefinition item = NodeBehaviorDefinitionRegistry.get(NodeType.PARAM_ITEM);
        NodeBehaviorDefinition block = NodeBehaviorDefinitionRegistry.get(NodeType.PARAM_BLOCK);
        NodeBehaviorDefinition entity = NodeBehaviorDefinitionRegistry.get(NodeType.PARAM_ENTITY);
        NodeBehaviorDefinition player = NodeBehaviorDefinitionRegistry.get(NodeType.PARAM_PLAYER);

        assertNotNull(item);
        assertNotNull(block);
        assertNotNull(entity);
        assertNotNull(player);

        assertTrue(item.hasParameterBehavior());
        assertTrue(item.hasRuntimeBehavior());
        assertTrue(item.hasListEntryBehavior());
        assertTrue(item.hasGotoFallbackTargetBehavior());

        assertTrue(block.hasParameterBehavior());
        assertTrue(block.hasRuntimeBehavior());
        assertTrue(block.hasGotoFallbackTargetBehavior());

        assertTrue(entity.hasRuntimeBehavior());
        assertTrue(entity.hasListEntryBehavior());
        assertTrue(entity.hasGotoFallbackTargetBehavior());

        assertTrue(player.hasRuntimeBehavior());
        assertTrue(player.hasListEntryBehavior());
        assertTrue(player.hasGotoFallbackTargetBehavior());
    }

    @Test
    void comparableAndRuntimeFamiliesAreAvailableThroughUnifiedDefinitions() {
        assertTrue(NodeBehaviorDefinitionRegistry.get(NodeType.PARAM_COORDINATE).hasRuntimeBehavior());
        assertTrue(NodeBehaviorDefinitionRegistry.get(NodeType.PARAM_COORDINATE).hasComparableBehavior());
        assertTrue(NodeBehaviorDefinitionRegistry.get(NodeType.PARAM_AMOUNT).hasParameterBehavior());
        assertTrue(NodeBehaviorDefinitionRegistry.get(NodeType.PARAM_AMOUNT).hasComparableBehavior());
        assertTrue(NodeBehaviorDefinitionRegistry.snapshot().containsKey(NodeType.PARAM_DIRECTION));
    }
}
