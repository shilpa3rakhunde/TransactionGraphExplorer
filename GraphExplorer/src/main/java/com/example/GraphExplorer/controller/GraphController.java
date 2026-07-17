package com.example.GraphExplorer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.GraphExplorer.dto.NodeDetailResponse;
import com.example.GraphExplorer.service.GraphService;

@RestController
@RequestMapping("/api/graph/nodes")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    /**
     * GET /api/graph/nodes/{id}
     * GET /api/graph/nodes/{id}?maxDepth=3
     * GET /api/graph/nodes/{id}?maxDepth=3&withAggregates=true
     */
    @GetMapping("/{id}")
    public NodeDetailResponse getNode(
            @PathVariable String id,
            @RequestParam(required = false) Integer maxDepth,
            @RequestParam(required = false, defaultValue = "false") boolean withAggregates) {
        return graphService.getNodeDetail(id, maxDepth, withAggregates);
    }

    /**
     * GET /api/graph/nodes/{id}/children-transactions?minAmount=100&maxAmount=5000&txnType=POS
     */
    @GetMapping("/{id}/children-transactions")
    public NodeDetailResponse getChildrenTransactions(
            @PathVariable String id,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String txnType) {
        return graphService.getFilteredChildrenTransactions(id, minAmount, maxAmount, txnType);
    }
}
