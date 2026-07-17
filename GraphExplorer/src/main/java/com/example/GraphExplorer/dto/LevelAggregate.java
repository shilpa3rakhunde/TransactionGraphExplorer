package com.example.GraphExplorer.dto;

public class LevelAggregate {

    private int level;
    private int nodeCount;
    private int transactionCount;
    private double totalAmount; // sum of absolute transaction amounts at this level

    public LevelAggregate(int level, int nodeCount, int transactionCount, double totalAmount) {
        this.level = level;
        this.nodeCount = nodeCount;
        this.transactionCount = transactionCount;
        this.totalAmount = totalAmount;
    }

    public int getLevel() {
        return level;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }
}
