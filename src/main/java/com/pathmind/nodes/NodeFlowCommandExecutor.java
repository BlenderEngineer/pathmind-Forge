package com.pathmind.nodes;

import com.pathmind.data.NodeGraphData;
import com.pathmind.data.NodeGraphPersistence;
import com.pathmind.data.PresetManager;
import com.pathmind.execution.ExecutionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

final class NodeFlowCommandExecutor {
    private final Node owner;
    private final NodeType type;

    NodeFlowCommandExecutor(Node owner) {
        this.owner = owner;
        this.type = owner.getType();
    }

    void executeStopChainNode(CompletableFuture<Void> future) {
        Node owningStart = owner.getOwningStartNode();
        ExecutionManager manager = ExecutionManager.getInstance();
        int targetNumber = owner.getIntParameter("StartNumber", 0);

        if (targetNumber > 0) {
            boolean stopped = manager.requestStopForStartNumber(targetNumber);
            if (!stopped) {
                manager.requestStopAll();
            }
            future.complete(null);
            return;
        }

        if (owningStart == null) {
            manager.requestStopAll();
        } else {
            boolean stopped = manager.requestStopForStart(owningStart);
            if (!stopped) {
                manager.requestStopAll();
            }
        }

        future.complete(null);
    }

    void executeStartChainNode(CompletableFuture<Void> future) {
        int targetNumber = owner.getIntParameter("StartNumber", 0);
        if (targetNumber <= 0) {
            future.complete(null);
            return;
        }

        ExecutionManager manager = ExecutionManager.getInstance();
        manager.requestStartForStartNumber(targetNumber);
        future.complete(null);
    }

    void executeRunPresetNode(CompletableFuture<Void> future) {
        String requestedPreset = owner.getStringParameter("Preset", "");
        String presetName = requestedPreset != null ? requestedPreset.trim() : "";
        if (presetName.isEmpty()) {
            presetName = PresetManager.getActivePreset();
        }

        List<String> availablePresets = PresetManager.getAvailablePresets();
        for (String available : availablePresets) {
            if (available != null && available.equalsIgnoreCase(presetName)) {
                presetName = available;
                break;
            }
        }

        NodeGraphData graphData = NodeGraphPersistence.loadNodeGraphForPreset(presetName);
        if (graphData == null || graphData.getNodes() == null || graphData.getNodes().isEmpty()) {
            future.complete(null);
            return;
        }

        List<Node> nodes = NodeGraphPersistence.convertToNodes(graphData);
        if (nodes == null || nodes.isEmpty()) {
            future.complete(null);
            return;
        }
        Map<String, Node> nodeMap = new HashMap<>();
        for (Node node : nodes) {
            if (node != null && node.getId() != null) {
                nodeMap.put(node.getId(), node);
            }
        }
        List<NodeConnection> connections = NodeGraphPersistence.convertToConnections(graphData, nodeMap);

        List<Node> presetStarts = new ArrayList<>();
        for (Node candidate : nodes) {
            if (candidate != null && candidate.getType() == NodeType.START) {
                presetStarts.add(candidate);
            }
        }
        if (presetStarts.isEmpty()) {
            future.complete(null);
            return;
        }

        ExecutionManager manager = ExecutionManager.getInstance();
        int started = 0;
        List<CompletableFuture<Void>> nestedFutures = new ArrayList<>();
        for (Node startNode : presetStarts) {
            if (type == NodeType.CUSTOM_NODE || type == NodeType.TEMPLATE) {
                CompletableFuture<Void> nestedFuture = manager.executeExternalBranchAndWait(startNode, nodes, connections, presetName);
                if (nestedFuture != null) {
                    started++;
                    nestedFutures.add(nestedFuture);
                }
            } else if (manager.executeExternalBranch(startNode, nodes, connections, presetName)) {
                started++;
            }
        }

        if (started == 0) {
            future.complete(null);
        } else if (type == NodeType.CUSTOM_NODE || type == NodeType.TEMPLATE) {
            CompletableFuture.allOf(nestedFutures.toArray(new CompletableFuture[0]))
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                    } else {
                        future.complete(null);
                    }
                });
        } else {
            future.complete(null);
        }
    }

    void executeStopAllNode(CompletableFuture<Void> future) {
        ExecutionManager.getInstance().requestStopAll();
        future.complete(null);
    }
}
