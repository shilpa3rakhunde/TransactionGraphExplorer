package com.example.GraphExplorer.dto;

import java.util.List;

import com.example.GraphExplorer.model.NodeTransaction;

public class ChildTreeNode {

    private String id;
    private String name;
    private String accountNumber;
    private int level;
    private List<NodeTransaction> transactions;
    private List<ChildTreeNode> children;

    public ChildTreeNode(String id, String name, String accountNumber, int level,
                          List<NodeTransaction> transactions, List<ChildTreeNode> children) {
        this.id = id;
        this.name = name;
        this.accountNumber = accountNumber;
        this.level = level;
        this.transactions = transactions;
        this.children = children;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public int getLevel() {
        return level;
    }

    public List<NodeTransaction> getTransactions() {
        return transactions;
    }

    public List<ChildTreeNode> getChildren() {
        return children;
    }
}
