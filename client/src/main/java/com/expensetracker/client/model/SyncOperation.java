package com.expensetracker.client.model;

public class SyncOperation {
    private int id;
    private String operationType; // INSERT, UPDATE, DELETE
    private String entityType; // TRANSACTION, CATEGORY
    private String payload; // JSON representation
    private long timestamp;
    private int retryCount;

    public SyncOperation() {
    }

    public SyncOperation(String operationType, String entityType, String payload) {
        this.operationType = operationType;
        this.entityType = entityType;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
        this.retryCount = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
