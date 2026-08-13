package com.bookstore.model;

import com.bookstore.dto.ImportProgressDTO;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "import_jobs")
public class ImportJob {
    @Id
    private String id; // importId

    @Indexed
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
    private Instant updatedAt;
    private Instant completedAt;

    public ImportJob() {}

    public ImportJob(String id, String sellerId, int total, String status, String message) {
        this.id = id;
        this.sellerId = sellerId;
        this.total = total;
        this.processed = 0;
        this.added = 0;
        this.updated = 0;
        this.failed = 0;
        this.percentage = 0;
        this.status = status;
        this.message = message;
        this.startedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public ImportProgressDTO toDTO() {
        return new ImportProgressDTO(
            id, sellerId, total, processed, added, updated, failed, percentage, status, message, startedAt, completedAt
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Instant getUpdatedAt() {
        return updatedAt != null ? updatedAt : startedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
