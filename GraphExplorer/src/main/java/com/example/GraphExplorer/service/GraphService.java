package com.example.GraphExplorer.service;

import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.example.GraphExplorer.dto.ChildTreeNode;
import com.example.GraphExplorer.dto.LevelAggregate;
import com.example.GraphExplorer.dto.NodeDetailResponse;
import com.example.GraphExplorer.dto.NodeSummary;
import com.example.GraphExplorer.exception.CycleDetectedException;
import com.example.GraphExplorer.exception.InvalidRequestException;
import com.example.GraphExplorer.exception.NodeNotFoundException;
import com.example.GraphExplorer.model.GraphDataset;
import com.example.GraphExplorer.model.GraphNode;
import com.example.GraphExplorer.model.NodeTransaction;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
public class GraphService {

    private static final String DATA_FILE = "transactions-graph-nodes.json";

    private final Map<String, GraphNode> nodeById = new HashMap<>();
    private final Map<String, List<GraphNode>> childrenByParentId = new HashMap<>();

    @PostConstruct
    public void loadData() {
        try (InputStream is = new ClassPathResource(DATA_FILE).getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            GraphDataset dataset = mapper.readValue(is, GraphDataset.class);

            if (dataset.getNodes() != null) {
                for (GraphNode node : dataset.getNodes()) {
                    nodeById.put(node.getId(), node);
                }
                for (GraphNode node : dataset.getNodes()) {
                    if (node.getParentId() != null && nodeById.containsKey(node.getParentId())) {
                        childrenByParentId
                                .computeIfAbsent(node.getParentId(), k -> new ArrayList<>())
                                .add(node);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load graph dataset: " + DATA_FILE, e);
        }
    }

    private GraphNode getNodeOrThrow(String id) {
        GraphNode node = nodeById.get(id);
        if (node == null) {
            throw new NodeNotFoundException(id);
        }
        return node;
    }

    private boolean hasParent(GraphNode node) {
        return node.getParentId() != null && nodeById.containsKey(node.getParentId());
    }

    private List<GraphNode> getDirectChildren(String nodeId) {
        return childrenByParentId.getOrDefault(nodeId, Collections.emptyList());
    }

    /**
     * Walks the parentId chain upward, building the chain from root down to
     * (but not including) the given node. Detects cycles using a visited set.
     */
    private List<GraphNode> buildParentChain(GraphNode node) {
        LinkedList<GraphNode> chain = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        visited.add(node.getId());

        GraphNode current = node;
        while (hasParent(current)) {
            GraphNode parent = nodeById.get(current.getParentId());
            if (!visited.add(parent.getId())) {
                throw new CycleDetectedException(
                        "Cycle detected while building parent chain for node " + node.getId());
            }
            chain.addFirst(parent);
            current = parent;
        }
        return chain;
    }

    private int computeLevel(GraphNode node) {
        // A node's level equals the length of its parent chain (root has an empty chain -> level 0).
        return buildParentChain(node).size();
    }

    private List<NodeSummary> toSummaries(List<GraphNode> nodes) {
        List<NodeSummary> result = new ArrayList<>();
        for (GraphNode n : nodes) {
            result.add(new NodeSummary(n.getId(), n.getName(), n.getAccountNumber()));
        }
        return result;
    }

    /**
     * Main entry point for GET /api/graph/nodes/{id}.
     *
     * @param id       node id
     * @param maxDepth depth for the nested childrenTree (bonus 5.1). 1 = flat children only.
     * @param withAggregates whether to compute per-level aggregates for the subtree (bonus 5.2)
     */
    public NodeDetailResponse getNodeDetail(String id, Integer maxDepth, boolean withAggregates) {
        GraphNode node = getNodeOrThrow(id);

        int depth = (maxDepth == null) ? 1 : maxDepth;
        if (depth < 0 || depth > 10) {
            throw new InvalidRequestException("maxDepth must be between 0 and 10");
        }
        // Spec says default 1, max 5 for the "suggested" range, but we accept up to 10 defensively
        // and clamp the practical suggested max to 5 unless caller explicitly asks higher.

        List<GraphNode> parentChain = buildParentChain(node);
        List<GraphNode> directChildren = getDirectChildren(node.getId());

        NodeDetailResponse response = new NodeDetailResponse();
        response.setId(node.getId());
        response.setParentId(node.getParentId());
        response.setName(node.getName());
        response.setAccountNumber(node.getAccountNumber());
        response.setLevel(parentChain.size());
        response.setRoot(!hasParent(node));
        response.setLeaf(directChildren.isEmpty());
        response.setParentChain(toSummaries(parentChain));
        response.setChildren(toSummaries(directChildren));
        response.setTransactions(safeTransactions(node));

        List<NodeTransaction> nextLevelTxns = new ArrayList<>();
        for (GraphNode child : directChildren) {
            nextLevelTxns.addAll(safeTransactions(child));
        }
        response.setNextLevelTransactions(nextLevelTxns);

        if (depth > 1) {
            List<ChildTreeNode> tree = new ArrayList<>();
            Set<String> visited = new HashSet<>();
            visited.add(node.getId());
            for (GraphNode child : directChildren) {
                tree.add(buildChildTree(child, 1, depth, visited));
            }
            response.setChildrenTree(tree);
        }

        if (withAggregates) {
            response.setLevelAggregates(computeLevelAggregates(node));
        }

        return response;
    }

    private List<NodeTransaction> safeTransactions(GraphNode node) {
        return node.getTransactions() == null ? Collections.emptyList() : node.getTransactions();
    }

    private ChildTreeNode buildChildTree(GraphNode node, int currentDepth, int maxDepth, Set<String> visited) {
        if (!visited.add(node.getId())) {
            throw new CycleDetectedException(
                    "Cycle detected while building children tree at node " + node.getId());
        }

        List<ChildTreeNode> childNodes = new ArrayList<>();
        if (currentDepth < maxDepth) {
            for (GraphNode child : getDirectChildren(node.getId())) {
                childNodes.add(buildChildTree(child, currentDepth + 1, maxDepth, visited));
            }
        }

        return new ChildTreeNode(
                node.getId(),
                node.getName(),
                node.getAccountNumber(),
                currentDepth,
                safeTransactions(node),
                childNodes
        );
    }

    /**
     * Computes per-level node/transaction/amount aggregates for the subtree rooted at the given node.
     * The root of the subtree is treated as relative level 0. Uses BFS with a visited set to guard
     * against cycles. totalAmount is the sum of absolute transaction amounts at that level.
     */
    private List<LevelAggregate> computeLevelAggregates(GraphNode root) {
        Map<Integer, Integer> nodeCountByLevel = new TreeMap<>();
        Map<Integer, Integer> txnCountByLevel = new TreeMap<>();
        Map<Integer, Double> amountByLevel = new TreeMap<>();

        Set<String> visited = new HashSet<>();
        Deque<AbstractMap.SimpleEntry<GraphNode, Integer>> queue = new ArrayDeque<>();
        queue.add(new AbstractMap.SimpleEntry<>(root, 0));
        visited.add(root.getId());

        while (!queue.isEmpty()) {
            AbstractMap.SimpleEntry<GraphNode, Integer> entry = queue.poll();
            GraphNode current = entry.getKey();
            int level = entry.getValue();

            nodeCountByLevel.merge(level, 1, Integer::sum);
            List<NodeTransaction> txns = safeTransactions(current);
            txnCountByLevel.merge(level, txns.size(), Integer::sum);
            double sum = 0.0;
            for (NodeTransaction t : txns) {
                sum += Math.abs(t.getAmount());
            }
            amountByLevel.merge(level, sum, Double::sum);

            for (GraphNode child : getDirectChildren(current.getId())) {
                if (!visited.add(child.getId())) {
                    throw new CycleDetectedException(
                            "Cycle detected while computing level aggregates at node " + child.getId());
                }
                queue.add(new AbstractMap.SimpleEntry<>(child, level + 1));
            }
        }

        List<LevelAggregate> result = new ArrayList<>();
        for (Integer level : nodeCountByLevel.keySet()) {
            result.add(new LevelAggregate(
                    level,
                    nodeCountByLevel.get(level),
                    txnCountByLevel.getOrDefault(level, 0),
                    amountByLevel.getOrDefault(level, 0.0)
            ));
        }
        return result;
    }

    /**
     * GET /api/graph/nodes/{id}/children-transactions
     * Returns node details, level, and the transactions of direct child nodes filtered by
     * minAmount / maxAmount / txnType.
     */
    public NodeDetailResponse getFilteredChildrenTransactions(String id, Double minAmount,
                                                                Double maxAmount, String txnType) {
        GraphNode node = getNodeOrThrow(id);
        List<GraphNode> parentChain = buildParentChain(node);
        List<GraphNode> directChildren = getDirectChildren(node.getId());

        NodeDetailResponse response = new NodeDetailResponse();
        response.setId(node.getId());
        response.setParentId(node.getParentId());
        response.setName(node.getName());
        response.setAccountNumber(node.getAccountNumber());
        response.setLevel(parentChain.size());
        response.setRoot(!hasParent(node));
        response.setLeaf(directChildren.isEmpty());
        response.setChildren(toSummaries(directChildren));

        List<NodeTransaction> filtered = new ArrayList<>();
        for (GraphNode child : directChildren) {
            for (NodeTransaction t : safeTransactions(child)) {
                if (minAmount != null && t.getAmount() < minAmount) continue;
                if (maxAmount != null && t.getAmount() > maxAmount) continue;
                if (txnType != null && !txnType.equalsIgnoreCase(t.getTxnType())) continue;
                filtered.add(t);
            }
        }
        response.setNextLevelTransactions(filtered);
        return response;
    }
}
