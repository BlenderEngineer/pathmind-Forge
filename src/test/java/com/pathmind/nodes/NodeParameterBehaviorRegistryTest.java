package com.pathmind.nodes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeParameterBehaviorRegistryTest {

    @Test
    void parameterBehaviorsAreRegisteredForInitialFamily() {
        assertNotNull(NodeParameterBehaviorRegistry.get(NodeType.PARAM_AMOUNT));
        assertNotNull(NodeParameterBehaviorRegistry.get(NodeType.PARAM_ITEM));
        assertNotNull(NodeParameterBehaviorRegistry.get(NodeType.PARAM_BLOCK_FACE));
        assertNotNull(NodeParameterBehaviorRegistry.get(NodeType.PARAM_DURATION));
        assertNotNull(NodeParameterBehaviorRegistry.get(NodeType.PARAM_BOOLEAN));
        assertNotNull(NodeParameterBehaviorRegistry.get(NodeType.PARAM_DIRECTION));
        assertNotNull(NodeParameterBehaviorRegistry.get(NodeType.PARAM_INVENTORY_SLOT));
        assertTrue(NodeParameterBehaviorRegistry.snapshot().containsKey(NodeType.PARAM_HAND));
    }

    @Test
    void amountParameterExportsNumericAliases() {
        Node amount = new Node(NodeType.PARAM_AMOUNT, 0, 0);
        amount.getParameter("Amount").setStringValue("5");

        Map<String, String> values = amount.exportParameterValues();

        assertEquals("5", values.get("Amount"));
        assertEquals("5", values.get("Count"));
        assertEquals("5", values.get("Threshold"));
        assertEquals("5", values.get("Value"));
        assertEquals("5", values.get("count"));
    }

    @Test
    void itemParameterSyncsSingularAndPluralAliases() {
        Node item = new Node(NodeType.PARAM_ITEM, 0, 0);
        item.getParameter("Item").setStringValue("diamond");

        Map<String, String> values = item.exportParameterValues();

        assertEquals("diamond", values.get("Item"));
        assertEquals("diamond", values.get("Items"));
    }

    @Test
    void blockFaceParameterExportsDirectionAliases() {
        Node face = new Node(NodeType.PARAM_BLOCK_FACE, 0, 0);
        face.getParameter("Face").setStringValue("north");

        Map<String, String> values = face.exportParameterValues();

        assertEquals("north", values.get("Face"));
        assertEquals("north", values.get("Side"));
        assertEquals("north", values.get("Direction"));
    }

    @Test
    void durationParameterExportsSecondsAliases() {
        Node duration = new Node(NodeType.PARAM_DURATION, 0, 0);
        duration.setMode(NodeMode.WAIT_MINUTES);
        duration.getParameter("Duration").setStringValue("2");

        Map<String, String> values = duration.exportParameterValues();

        assertEquals("120.0", values.get("Duration"));
        assertEquals("120.0", values.get("IntervalSeconds"));
        assertEquals("120.0", values.get("WaitSeconds"));
        assertEquals("120.0", values.get("DurationSeconds"));
    }

    @Test
    void booleanParameterExportsResolvedToggleAliases() {
        Node bool = new Node(NodeType.PARAM_BOOLEAN, 0, 0);
        bool.setBooleanModeLiteral(true);
        bool.getParameter("Toggle").setStringValue("false");

        Map<String, String> values = bool.exportParameterValues();

        assertEquals("literal", values.get("Mode"));
        assertEquals("false", values.get("Toggle"));
        assertEquals("false", values.get("Active"));
        assertEquals("false", values.get("Enabled"));
    }

    @Test
    void cardinalDirectionParameterExportsOrientationAliases() {
        Node direction = new Node(NodeType.PARAM_DIRECTION, 0, 0);
        direction.setDirectionModeExact(false);
        direction.getParameter("Direction").setStringValue("west");

        Map<String, String> values = direction.exportParameterValues();

        assertEquals("cardinal", values.get("Mode"));
        assertEquals("90.0", values.get("Yaw"));
        assertEquals("west", values.get("Side"));
        assertEquals("west", values.get("Face"));
        assertEquals("west", values.get("Message"));
    }

    @Test
    void inventorySlotParameterExportsSlotAliases() {
        Node slot = new Node(NodeType.PARAM_INVENTORY_SLOT, 0, 0);
        slot.getParameter("Slot").setStringValue("3");

        Map<String, String> values = slot.exportParameterValues();

        assertEquals("3", values.get("Slot"));
        assertEquals("3", values.get("SourceSlot"));
        assertEquals("3", values.get("TargetSlot"));
        assertEquals("3", values.get("FirstSlot"));
        assertEquals("3", values.get("SecondSlot"));
    }
}
