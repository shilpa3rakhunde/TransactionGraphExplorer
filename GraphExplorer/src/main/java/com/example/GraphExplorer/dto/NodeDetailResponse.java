package com.example.GraphExplorer.dto;

import com.example.GraphExplorer.model.NodeTransaction;

import java.util.List;

public class NodeDetailResponse {

    private String id;
    private String parentId;
    private String name;
    private String accountNumber;
    private int level;
    private boolean isRoot;
    private boolean isLeaf;

    private List<NodeSummary> parentChain;
    private List<NodeSummary> children;

    private List<NodeTransaction> transactions;
    private List<NodeTransaction> nextLevelTransactions;

    // Populated only when maxDepth > 1 (bonus 5.1)
    private List<ChildTreeNode> childrenTree;

    // Populated only when levelAggregates are requested (bonus 5.2)
    private List<LevelAggregate> levelAggregates;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setRoot(boolean root) {
        isRoot = root;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }

    public List<NodeSummary> getParentChain() {
        return parentChain;
    }

    public void setParentChain(List<NodeSummary> parentChain) {
        this.parentChain = parentChain;
    }

    public List<NodeSummary> getChildren() {
        return children;
    }

    public void setChildren(List<NodeSummary> children) {
        this.children = children;
    }

    public List<NodeTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<NodeTransaction> transactions) {
        this.transactions = transactions;
    }

    public List<NodeTransaction> getNextLevelTransactions() {
        return nextLevelTransactions;
    }

    public void setNextLevelTransactions(List<NodeTransaction> nextLevelTransactions) {
        this.nextLevelTransactions = nextLevelTransactions;
    }

    public List<ChildTreeNode> getChildrenTree() {
        return childrenTree;
    }

    public void setChildrenTree(List<ChildTreeNode> childrenTree) {
        this.childrenTree = childrenTree;
    }

    public List<LevelAggregate> getLevelAggregates() {
        return levelAggregates;
    }

    public void setLevelAggregates(List<LevelAggregate> levelAggregates) {
        this.levelAggregates = levelAggregates;
    }
}
