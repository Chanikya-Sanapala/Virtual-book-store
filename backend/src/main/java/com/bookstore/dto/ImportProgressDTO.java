package com.bookstore.dto;

import java.time.Instant;

public class ImportProgressDTO {
    private String importId;
    private String sellerId;
    private int total;
    private int processed;
    private int added;
    private int updated;
    private int failed;
    private int percentage;
    private String status; // STARTING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    private String message;
    private Instant startedAt;
    private Instant completedAt;

    public ImportProgressDTO() {}

    public ImportProgressDTO(String importId, String sellerId, int total, int processed, int added, int updated, int failed, int percentage, String status, String message, Instant startedAt, Instant completedAt) {
        this.importId = importId;
        this.sellerId = sellerId;
        this.total = total;
        this.processed = processed;
        this.added = added;
        this.updated = updated;
        this.failed = failed;
        this.percentage = percentage;
        this.status = status;
        this.message = message;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public String getImportId() {
        return importId;
    }

    public void setImportId(String importId) {
        this.importId = importId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getProcessed() {
        return processed;
    }

    public void setProcessed(int processed) {
        this.processed = processed;
    }

    public int getAdded() {
        return added;
    }

    public void setAdded(int added) {
        this.added = added;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
