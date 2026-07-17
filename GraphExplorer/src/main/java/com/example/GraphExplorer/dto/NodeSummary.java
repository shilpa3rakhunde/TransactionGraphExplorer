package com.example.GraphExplorer.dto;

public class NodeSummary {

    private String id;
    private String name;
    private String accountNumber;

    public NodeSummary(String id, String name, String accountNumber) {
        this.id = id;
        this.name = name;
        this.accountNumber = accountNumber;
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
}
