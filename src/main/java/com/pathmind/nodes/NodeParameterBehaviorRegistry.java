package com.pathmind.nodes;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

final class NodeParameterBehaviorRegistry {
    private static final Map<NodeType, NodeParameterBehavior> BEHAVIORS = new EnumMap<>(NodeType.class);

    static {
        register(NodeType.PARAM_DURATION, (node, values) -> {
            String duration = values.get("Duration");
            if (duration != null) {
                String secondsValue = Double.toString(parseNonNegativeDouble(duration, 1.0) * durationUnitSeconds(node.getMode()));
                put(values, "Duration", secondsValue);
                put(values, "IntervalSeconds", secondsValue);
                put(values, "WaitSeconds", secondsValue);
                put(values, "DurationSeconds", secondsValue);
            }
            return values;
        });
        register(NodeType.PARAM_AMOUNT, (node, values) -> {
            String amount = values.get("Amount");
            if (amount != null) {
                put(values, "Count", amount);
                put(values, "Threshold", amount);
                put(values, "Value", amount);
            }
            return values;
        });
        register(NodeType.PARAM_ITEM, (node, values) -> {
            syncSingularAndPlural(values, "Item", "Items");
            String amount = values.get("Amount");
            if (amount != null) {
                put(values, "Count", amount);
            }
            return values;
        });
        register(NodeType.PARAM_BLOCK, (node, values) -> {
            syncSingularAndPlural(values, "Block", "Blocks");
            return values;
        });
        register(NodeType.PARAM_VILLAGER_TRADE, (node, values) -> {
            String item = values.get("Item");
            if (item != null && !item.isEmpty()) {
                put(values, "Items", item);
            }
            return values;
        });
        register(NodeType.PARAM_INVENTORY_SLOT, (node, values) -> {
            String slot = values.get("Slot");
            if (slot != null) {
                put(values, "SourceSlot", slot);
                put(values, "TargetSlot", slot);
                put(values, "FirstSlot", slot);
                put(values, "SecondSlot", slot);
            }
            ItemStack resolvedStack = InventorySlotValueResolver.resolveComparableInventorySlotStack(values);
            if (resolvedStack != null && !resolvedStack.isEmpty()) {
                Identifier itemId = Registries.ITEM.getId(resolvedStack.getItem());
                if (itemId != null) {
                    String itemValue = itemId.toString();
                    put(values, "Item", itemValue);
                    put(values, "Items", itemValue);
                }
                String countValue = Integer.toString(resolvedStack.getCount());
                put(values, "Count", countValue);
                put(values, "Amount", countValue);
            }
            return values;
        });
        register(NodeType.PARAM_PLAYER, (node, values) -> copyIfPresent(values, "Player", "Name"));
        register(NodeType.PARAM_WAYPOINT, (node, values) -> copyIfPresent(values, "Waypoint", "Name"));
        register(NodeType.PARAM_MESSAGE, (node, values) -> copyIfPresent(values, "Text", "Message"));
        register(NodeType.PARAM_ENTITY, (node, values) -> copyIfPresent(values, "Range", "Distance"));
        register(NodeType.PARAM_RANGE, (node, values) -> {
            String range = values.get("Range");
            if (range != null) {
                put(values, "Distance", range);
                put(values, "Radius", range);
            }
            return values;
        });
        register(NodeType.PARAM_CLOSEST, (node, values) -> copyIfPresent(values, "Range", "Distance"));
        register(NodeType.PARAM_HAND, (node, values) -> {
            String hand = values.get("Hand");
            if (hand != null) {
                put(values, "SourceHand", hand);
                put(values, "TargetHand", hand);
                put(values, "SelectedHand", hand);
            }
            return values;
        });
        register(NodeType.PARAM_PLACE_TARGET, (node, values) -> copyIfPresent(values, "Block", "BlockId"));
        register(NodeType.PARAM_ROTATION, (node, values) -> {
            String yaw = values.get("Yaw");
            if (yaw != null) {
                put(values, "YawOffset", yaw);
            }
            String pitch = values.get("Pitch");
            if (pitch != null) {
                put(values, "PitchOffset", pitch);
            }
            return values;
        });
        register(NodeType.PARAM_BLOCK_FACE, (node, values) -> {
            String face = values.get("Face");
            if (face != null && !face.trim().isEmpty()) {
                put(values, "Side", face);
                put(values, "Direction", face);
            }
            return values;
        });
        register(NodeType.PARAM_BOOLEAN, (node, values) -> {
            node.ensureBooleanParameters();
            String modeValue = node.isBooleanModeLiteral() ? "literal" : "variable";
            put(values, "Mode", modeValue);
            NodeParameter variableParameter = node.getParameter("Variable");
            if (variableParameter != null) {
                put(values, "Variable", variableParameter.getStringValue());
            }
            Optional<Boolean> resolvedToggle = node.resolveBooleanNodeValue(node);
            String toggle = resolvedToggle.map(String::valueOf).orElseGet(() -> values.get("Toggle"));
            if (toggle == null) {
                toggle = values.get(Node.normalizeParameterKey("Toggle"));
            }
            if (toggle != null) {
                put(values, "Active", toggle);
                put(values, "Enabled", toggle);
                put(values, "Toggle", toggle);
            }
            return values;
        });
        register(NodeType.PARAM_DIRECTION, (node, values) -> {
            String modeValue = node.isDirectionModeExact() ? "exact" : "cardinal";
            put(values, "Mode", modeValue);
            String direction = values.get("Direction");
            if ("cardinal".equals(modeValue) && direction != null && !direction.trim().isEmpty()) {
                applyCardinalDirection(values, direction);
            }
            return values;
        });
    }

    static NodeParameterBehavior get(NodeType type) {
        return BEHAVIORS.get(type);
    }

    static Map<NodeType, NodeParameterBehavior> snapshot() {
        return new EnumMap<>(BEHAVIORS);
    }

    private static void register(NodeType type, NodeParameterBehavior behavior) {
        BEHAVIORS.put(type, behavior);
    }

    private static Map<String, String> copyIfPresent(Map<String, String> values, String sourceKey, String targetKey) {
        String value = values.get(sourceKey);
        if (value != null) {
            put(values, targetKey, value);
        }
        return values;
    }

    private static void syncSingularAndPlural(Map<String, String> values, String singularKey, String pluralKey) {
        String singular = values.get(singularKey);
        String plural = values.get(pluralKey);
        if ((plural == null || plural.isEmpty()) && singular != null && !singular.isEmpty()) {
            put(values, pluralKey, singular);
            return;
        }
        if ((singular == null || singular.isEmpty()) && plural != null && !plural.isEmpty()) {
            String first = firstCommaSeparatedEntry(plural);
            if (first != null && !first.isEmpty()) {
                put(values, singularKey, first);
            }
        }
    }

    private static double parseNonNegativeDouble(String value, double defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        double parsed = defaultValue;
        if (!trimmed.isEmpty()) {
            try {
                parsed = Double.parseDouble(trimmed);
            } catch (NumberFormatException ignored) {
                parsed = 0.0;
            }
        }
        return Math.max(0.0, parsed);
    }

    private static double durationUnitSeconds(NodeMode mode) {
        NodeMode durationMode = mode != null ? mode : NodeMode.WAIT_SECONDS;
        return switch (durationMode) {
            case WAIT_TICKS -> 0.05;
            case WAIT_MINUTES -> 60.0;
            case WAIT_HOURS -> 3600.0;
            case WAIT_SECONDS -> 1.0;
            default -> 1.0;
        };
    }

    private static void applyCardinalDirection(Map<String, String> values, String direction) {
        String normalized = direction.trim().toLowerCase(Locale.ROOT);
        Double yaw = null;
        Double pitch = null;
        switch (normalized) {
            case "north":
                yaw = 180.0;
                break;
            case "south":
                yaw = 0.0;
                break;
            case "west":
                yaw = 90.0;
                break;
            case "east":
                yaw = -90.0;
                break;
            case "up":
                pitch = -90.0;
                break;
            case "down":
                pitch = 90.0;
                break;
            default:
                break;
        }
        if (yaw != null) {
            put(values, "Yaw", Double.toString(yaw));
        }
        if (pitch != null) {
            put(values, "Pitch", Double.toString(pitch));
        }
        put(values, "Side", direction);
        put(values, "Face", direction);
        put(values, "Text", direction);
        put(values, "Message", direction);
    }

    private static String firstCommaSeparatedEntry(String value) {
        for (String entry : value.split(",")) {
            String trimmed = entry == null ? null : entry.trim();
            if (trimmed != null && !trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return null;
    }

    private static void put(Map<String, String> values, String key, String value) {
        values.put(key, value);
        values.put(Node.normalizeParameterKey(key), value);
    }

    private NodeParameterBehaviorRegistry() {
    }
}
