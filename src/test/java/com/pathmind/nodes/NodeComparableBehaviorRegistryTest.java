package com.pathmind.nodes;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeComparableBehaviorRegistryTest {

    @Test
    void comparableBehaviorsAreRegisteredForInitialFamilies() {
        assertNotNull(NodeComparableBehaviorRegistry.get(NodeType.PARAM_COORDINATE));
        assertNotNull(NodeComparableBehaviorRegistry.get(NodeType.PARAM_MESSAGE));
        assertNotNull(NodeComparableBehaviorRegistry.get(NodeType.PARAM_AMOUNT));
        assertNotNull(NodeComparableBehaviorRegistry.get(NodeType.SENSOR_LOOK_DIRECTION));
        assertTrue(NodeComparableBehaviorRegistry.snapshot().containsKey(NodeType.PARAM_DISTANCE));
    }

    @Test
    void coordinateComparableBehaviorFormatsStringValue() {
        Node owner = new Node(NodeType.CONTROL_IF, 0, 0);
        Node coordinate = new Node(NodeType.PARAM_COORDINATE, 0, 0);
        coordinate.getParameter("X").setStringValue("1");
        coordinate.getParameter("Y").setStringValue("64");
        coordinate.getParameter("Z").setStringValue("-2");

        Optional<String> resolved = NodeComparableBehaviorRegistry
            .get(NodeType.PARAM_COORDINATE)
            .resolveString(owner, coordinate);

        assertEquals(Optional.of("1 64 -2"), resolved);
    }

    @Test
    void amountComparableBehaviorResolvesNumericValue() {
        Node owner = new Node(NodeType.CONTROL_IF, 0, 0);
        Node amount = new Node(NodeType.PARAM_AMOUNT, 0, 0);
        amount.getParameter("Amount").setStringValue("12.5");

        Optional<Double> resolved = NodeComparableBehaviorRegistry
            .get(NodeType.PARAM_AMOUNT)
            .resolveNumber(owner, amount);

        assertEquals(Optional.of(12.5), resolved);
    }

    @Test
    void directionComparableBehaviorPrefersExportedOrientation() {
        Node owner = new Node(NodeType.CONTROL_IF, 0, 0);
        Node direction = new Node(NodeType.PARAM_DIRECTION, 0, 0);
        direction.setDirectionModeExact(false);
        direction.getParameter("Direction").setStringValue("north");

        Optional<String> resolved = NodeComparableBehaviorRegistry
            .get(NodeType.PARAM_DIRECTION)
            .resolveString(owner, direction);

        assertEquals(Optional.of("180.0 0.0"), resolved);
    }
}
