package com.pathmind.nodes;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

final class NodeComparableBehaviorRegistry {
    private static final Map<NodeType, NodeComparableBehavior> BEHAVIORS = new EnumMap<>(NodeType.class);

    static {
        register(NodeType.PARAM_COORDINATE, stringBehavior((owner, node) -> {
            String formatted = owner.formatCoordinateValues(node.exportParameterValues());
            return formatted.isEmpty() ? Optional.empty() : Optional.of(formatted);
        }));
        register(NodeType.PARAM_ROTATION, stringBehavior((owner, node) -> {
            String formatted = owner.formatRotationValues(node.exportParameterValues());
            return formatted.isEmpty() ? Optional.empty() : Optional.of(formatted);
        }));
        register(NodeType.PARAM_MESSAGE, stringBehavior((owner, node) -> {
            String text = Node.getParameterString(node, "Text");
            if (text == null || text.trim().isEmpty()) {
                text = Node.getParameterString(node, "Message");
            }
            return text == null || text.trim().isEmpty() ? Optional.empty() : Optional.of(text.trim());
        }));
        register(NodeType.PARAM_DIRECTION, stringBehavior(NodeComparableBehaviorRegistry::resolveDirectionString));
        register(NodeType.PARAM_BLOCK_FACE, stringBehavior(NodeComparableBehaviorRegistry::resolveBlockFaceString));
        register(NodeType.SENSOR_TARGETED_BLOCK_FACE, stringBehavior((owner, node) -> {
            Map<String, String> values = node.exportParameterValues();
            String face = owner.getRuntimeValue(values, "face");
            if (face.isEmpty()) {
                face = owner.getRuntimeValue(values, "side");
            }
            return face.isEmpty() ? Optional.empty() : Optional.of(face.trim());
        }));
        register(NodeType.SENSOR_LOOK_DIRECTION, combinedBehavior(
            NodeComparableBehaviorRegistry::resolveLookDirectionString,
            NodeComparableBehaviorRegistry::resolveSingleAxisAmount
        ));
        register(NodeType.SENSOR_POSITION_OF, combinedBehavior(
            (owner, node) -> {
                if (node.isSensorPositionSingleAxisMode()) {
                    return Optional.empty();
                }
                String formatted = owner.formatCoordinateValues(node.exportParameterValues());
                return formatted.isEmpty() ? Optional.empty() : Optional.of(formatted);
            },
            NodeComparableBehaviorRegistry::resolveSingleAxisAmount
        ));
        register(NodeType.PARAM_AMOUNT, numberBehavior((owner, node) -> Optional.of(Node.parseNodeDouble(node, "Amount", 0.0))));
        register(NodeType.OPERATOR_RANDOM, numberBehavior((owner, node) -> Optional.of(Node.parseNodeDouble(node, "Amount", 0.0))));
        register(NodeType.OPERATOR_MOD, numberBehavior((owner, node) -> Optional.of(Node.parseNodeDouble(node, "Amount", 0.0))));
        register(NodeType.LIST_LENGTH, numberBehavior((owner, node) -> Optional.of(Node.parseNodeDouble(node, "Amount", 0.0))));
        register(NodeType.PARAM_DISTANCE, numberBehavior((owner, node) -> Optional.of(Node.parseNodeDouble(node, "Distance", 0.0))));
        register(NodeType.SENSOR_DISTANCE_BETWEEN, numberBehavior(NodeComparableBehaviorRegistry::resolveDistanceValue));
        register(NodeType.SENSOR_IS_ON_GROUND, numberBehavior(NodeComparableBehaviorRegistry::resolveDistanceValue));
        register(NodeType.SENSOR_SLOT_ITEM_COUNT, numberBehavior((owner, node) -> {
            Map<String, String> values = node.exportParameterValues();
            String amountValue = owner.getRuntimeValue(values, "amount");
            if (amountValue.isEmpty()) {
                amountValue = owner.getRuntimeValue(values, "count");
            }
            return amountValue.isEmpty() ? Optional.empty() : Optional.ofNullable(Node.parseDoubleOrNull(amountValue));
        }));
        register(NodeType.PARAM_INVENTORY_SLOT, numberBehavior((owner, node) ->
            owner.resolveInventorySlotCount(node).map(count -> (double) count)));
    }

    static NodeComparableBehavior get(NodeType type) {
        return BEHAVIORS.get(type);
    }

    static Map<NodeType, NodeComparableBehavior> snapshot() {
        return new EnumMap<>(BEHAVIORS);
    }

    private static void register(NodeType type, NodeComparableBehavior behavior) {
        BEHAVIORS.put(type, behavior);
    }

    private static Optional<String> resolveDirectionString(Node owner, Node node) {
        String formatted = owner.formatRotationValues(node.exportParameterValues());
        if (!formatted.isEmpty()) {
            return Optional.of(formatted);
        }
        String direction = Node.getParameterString(node, "Direction");
        if (direction == null || direction.trim().isEmpty()) {
            direction = Node.getParameterString(node, "Side");
        }
        if (direction == null || direction.trim().isEmpty()) {
            direction = Node.getParameterString(node, "Face");
        }
        return direction == null || direction.trim().isEmpty() ? Optional.empty() : Optional.of(direction.trim());
    }

    private static Optional<String> resolveBlockFaceString(Node owner, Node node) {
        String face = Node.getParameterString(node, "Face");
        if (face == null || face.trim().isEmpty()) {
            face = Node.getParameterString(node, "Side");
        }
        if (face == null || face.trim().isEmpty()) {
            face = Node.getParameterString(node, "Direction");
        }
        return face == null || face.trim().isEmpty() ? Optional.empty() : Optional.of(face.trim());
    }

    private static Optional<String> resolveLookDirectionString(Node owner, Node node) {
        String formatted = owner.formatRotationValues(node.exportParameterValues());
        if (!formatted.isEmpty()) {
            return Optional.of(formatted);
        }
        Map<String, String> values = node.exportParameterValues();
        String direction = owner.getRuntimeValue(values, "direction");
        if (direction.isEmpty()) {
            direction = owner.getRuntimeValue(values, "side");
        }
        if (direction.isEmpty()) {
            direction = owner.getRuntimeValue(values, "face");
        }
        return direction.isEmpty() ? Optional.empty() : Optional.of(direction.trim());
    }

    private static Optional<Double> resolveDistanceValue(Node owner, Node node) {
        String distanceValue = owner.getRuntimeValue(node.exportParameterValues(), "distance");
        return distanceValue.isEmpty() ? Optional.empty() : Optional.ofNullable(Node.parseDoubleOrNull(distanceValue));
    }

    private static Optional<Double> resolveSingleAxisAmount(Node owner, Node node) {
        boolean singleAxis = node.getType() == NodeType.SENSOR_POSITION_OF
            ? node.isSensorPositionSingleAxisMode()
            : node.isSensorLookSingleAxisMode();
        if (!singleAxis) {
            return Optional.empty();
        }
        Map<String, String> values = node.exportParameterValues();
        String amountValue = owner.getRuntimeValue(values, "amount");
        if (amountValue.isEmpty()) {
            amountValue = owner.getRuntimeValue(values, "value");
        }
        return amountValue.isEmpty() ? Optional.empty() : Optional.ofNullable(Node.parseDoubleOrNull(amountValue));
    }

    private static NodeComparableBehavior stringBehavior(ComparableStringResolver resolver) {
        return new NodeComparableBehavior() {
            @Override
            public Optional<String> resolveString(Node owner, Node node) {
                return resolver.resolve(owner, node);
            }
        };
    }

    private static NodeComparableBehavior numberBehavior(ComparableNumberResolver resolver) {
        return new NodeComparableBehavior() {
            @Override
            public Optional<Double> resolveNumber(Node owner, Node node) {
                return resolver.resolve(owner, node);
            }
        };
    }

    private static NodeComparableBehavior combinedBehavior(ComparableStringResolver stringResolver,
                                                           ComparableNumberResolver numberResolver) {
        return new NodeComparableBehavior() {
            @Override
            public Optional<String> resolveString(Node owner, Node node) {
                return stringResolver.resolve(owner, node);
            }

            @Override
            public Optional<Double> resolveNumber(Node owner, Node node) {
                return numberResolver.resolve(owner, node);
            }
        };
    }

    @FunctionalInterface
    private interface ComparableStringResolver {
        Optional<String> resolve(Node owner, Node node);
    }

    @FunctionalInterface
    private interface ComparableNumberResolver {
        Optional<Double> resolve(Node owner, Node node);
    }

    private NodeComparableBehaviorRegistry() {
    }
}
