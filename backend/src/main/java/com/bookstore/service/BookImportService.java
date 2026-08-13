package com.bookstore.service;

import com.bookstore.dto.ImportProgressDTO;
import com.bookstore.model.Book;
import com.bookstore.model.ImportJob;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.ImportJobRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.*;

@Service
public class BookImportService {
    private static final Logger logger = LoggerFactory.getLogger(BookImportService.class);
    private static final int BATCH_SIZE = 30;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ImportJobRepository importJobRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.books.api.key:}")
    private String googleApiKey;

    public ImportProgressDTO startImport(byte[] fileBytes, String sellerId) throws Exception {
        List<String[]> rows = readCsvRows(fileBytes);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty.");
        }

        String[] header = rows.get(0);
        int totalDataRows = rows.size() - 1;

        if (totalDataRows <= 0) {
            throw new IllegalArgumentException("CSV file contains no book data rows.");
        }

        HeaderIndices indices = parseHeader(header);
        if (indices.isbnIndex == -1 && indices.titleIndex == -1) {
            throw new IllegalArgumentException("CSV file must contain an 'isbn' or 'title' column. Found: " + String.join(", ", header));
        }

        String importId = UUID.randomUUID().toString();
        ImportJob job = new ImportJob(importId, sellerId, totalDataRows, "STARTING", "Initializing import for " + totalDataRows + " books...");
        importJobRepository.save(job);

        logger.info("Created import job {} for seller {} with total = {} data rows", importId, sellerId, totalDataRows);

        return job.toDTO();
    }

    @Async("importTaskExecutor")
    public void processImportAsync(String importId, byte[] fileBytes, String sellerId) {
        logger.info("Starting async background processing for importJob {}", importId);
        Optional<ImportJob> optJob = importJobRepository.findById(importId);
        if (optJob.isEmpty()) {
            logger.error("Import job {} not found for background execution", importId);
            return;
        }

        ImportJob job = optJob.get();
        job.setStatus("IN_PROGRESS");
        job.setMessage("Processing 0 of " + job.getTotal() + " books");
        importJobRepository.save(job);

        try {
            List<String[]> rows = readCsvRows(fileBytes);
            if (rows.size() <= 1) {
                job.setStatus("COMPLETED");
                job.setCompletedAt(Instant.now());
                job.setMessage("No data rows to process.");
                importJobRepository.save(job);
                return;
            }

            HeaderIndices indices = parseHeader(rows.get(0));
            List<String[]> dataRows = rows.subList(1, rows.size());

            Set<String> seenIsbns = new HashSet<>();
            Set<String> seenTitles = new HashSet<>();

            int addedCount = 0;
            int updatedCount = 0;
            int failedCount = 0;

            List<Book> batchToSave = new ArrayList<>();

            for (int i = 0; i < dataRows.size(); i++) {
                String[] line = dataRows.get(i);
                int rowNum = i + 2; // 1-based row index including header

                ParsedRow parsed = parseAndValidateRow(line, indices, sellerId, rowNum, seenIsbns, seenTitles);

                if (!parsed.valid) {
                    failedCount++;
                    logger.warn("ImportJob {} Row {}: Validation failed - {}", importId, rowNum, parsed.errorMessage);
                } else if (parsed.book != null) {
                    batchToSave.add(parsed.book);
                    if (parsed.isUpdate) {
                        updatedCount++;
                    } else {
                        addedCount++;
                    }
                }

                // Batch database write
                if (batchToSave.size() >= BATCH_SIZE || i == dataRows.size() - 1) {
                    if (!batchToSave.isEmpty()) {
                        bookRepository.saveAll(batchToSave);
                        batchToSave.clear();
                    }

                    int processedCount = addedCount + updatedCount + failedCount;
                    int percentage = (int) Math.round((processedCount * 100.0) / job.getTotal());

                    job.setProcessed(processedCount);
                    job.setAdded(addedCount);
                    job.setUpdated(updatedCount);
                    job.setFailed(failedCount);
                    job.setPercentage(Math.min(100, percentage));
                    job.setMessage("Processed " + processedCount + " of " + job.getTotal() + " books (" + addedCount + " added, " + updatedCount + " updated, " + failedCount + " failed)");
                    job.setUpdatedAt(Instant.now());

                    importJobRepository.save(job);
                }
            }

            // Final state update
            int finalProcessed = addedCount + updatedCount + failedCount;
            job.setProcessed(finalProcessed);
            job.setAdded(addedCount);
            job.setUpdated(updatedCount);
            job.setFailed(failedCount);
            job.setPercentage(100);
            job.setStatus("COMPLETED");
            job.setCompletedAt(Instant.now());
            job.setUpdatedAt(Instant.now());
            job.setMessage("Import completed successfully. " + finalProcessed + " rows processed (" + addedCount + " added, " + updatedCount + " updated, " + failedCount + " failed)");

            importJobRepository.save(job);
            logger.info("ImportJob {} completed successfully: {} added, {} updated, {} failed", importId, addedCount, updatedCount, failedCount);

        } catch (Exception e) {
            logger.error("Error during async import processing for importJob {}", importId, e);
            job.setStatus("FAILED");
            job.setCompletedAt(Instant.now());
            job.setUpdatedAt(Instant.now());
            job.setMessage("Import failed due to error: " + e.getMessage());
            importJobRepository.save(job);
        }
    }

    public ImportProgressDTO getImportProgress(String importId, String sellerId, boolean isAdmin) {
        Optional<ImportJob> optJob = importJobRepository.findById(importId);
        if (optJob.isEmpty()) {
            return null;
        }

        ImportJob job = optJob.get();
        if (!isAdmin && !job.getSellerId().equals(sellerId)) {
            throw new SecurityException("Unauthorized access to import status.");
        }

        return job.toDTO();
    }

    public ImportProgressDTO getActiveImportForSeller(String sellerId) {
        Optional<ImportJob> optJob = importJobRepository.findFirstBySellerIdAndStatusInOrderByStartedAtDesc(
            sellerId, List.of("STARTING", "IN_PROGRESS")
        );
        return optJob.map(ImportJob::toDTO).orElse(null);
    }

    @Value("${bookstore.import.stale-threshold-minutes:5}")
    private int staleThresholdMinutes = 5;

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void reconcileStaleJobsOnStartup() {
        reconcileStaleJobs();
    }

    public void reconcileStaleJobs() {
        logger.info("Running stale import job reconciliation (threshold: {} minutes)...", staleThresholdMinutes);
        List<ImportJob> activeJobs = importJobRepository.findByStatusIn(List.of("STARTING", "IN_PROGRESS"));
        Instant cutoff = Instant.now().minusSeconds(staleThresholdMinutes * 60L);

        for (ImportJob job : activeJobs) {
            Instant lastActive = job.getUpdatedAt() != null ? job.getUpdatedAt() : job.getStartedAt();
            if (lastActive != null && lastActive.isBefore(cutoff)) {
                job.setStatus("FAILED");
                job.setCompletedAt(Instant.now());
                job.setUpdatedAt(Instant.now());
                job.setMessage("Import interrupted because the backend stopped before completion.");
                importJobRepository.save(job);
                logger.warn("Reconciled stale import job {} (last active: {}) to FAILED", job.getId(), lastActive);
            }
        }
    }

    private List<String[]> readCsvRows(byte[] fileBytes) throws Exception {
        try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(new ByteArrayInputStream(fileBytes))).build()) {
            return reader.readAll();
        }
    }

    private static class HeaderIndices {
        int isbnIndex = -1;
        int priceIndex = -1;
        int stockIndex = -1;
        int titleIndex = -1;
        int authorIndex = -1;
        int categoryIndex = -1;
        int descriptionIndex = -1;
        int imageIndex = -1;
    }

    private HeaderIndices parseHeader(String[] header) {
        HeaderIndices idx = new HeaderIndices();
        for (int i = 0; i < header.length; i++) {
            String h = header[i].toLowerCase().replaceAll("[^a-z0-9]", "");
            if (h.equals("isbn13")) {
                idx.isbnIndex = i;
            } else if (h.contains("isbn") && idx.isbnIndex == -1) {
                idx.isbnIndex = i;
            } else if (h.equals("price") || h.contains("amount")) {
                idx.priceIndex = i;
            } else if (h.equals("stock") || h.contains("count") || h.contains("quantity")) {
                idx.stockIndex = i;
            } else if (h.equals("title") || h.contains("booktitle") || h.equals("originaltitle")) {
                idx.titleIndex = i;
            } else if (h.contains("author")) {
                idx.authorIndex = i;
            } else if (h.contains("category") || h.contains("genre") || h.contains("subject")) {
                idx.categoryIndex = i;
            } else if (h.contains("description") || h.contains("summary") || h.contains("synopsis")) {
                idx.descriptionIndex = i;
            } else if (h.contains("image") || h.contains("cover") || h.contains("thumbnail") || h.contains("picture")) {
                idx.imageIndex = i;
            }
        }
        return idx;
    }

    private static class ParsedRow {
        boolean valid;
        boolean isUpdate;
        Book book;
        String errorMessage;

        ParsedRow(boolean valid, boolean isUpdate, Book book, String errorMessage) {
            this.valid = valid;
            this.isUpdate = isUpdate;
            this.book = book;
            this.errorMessage = errorMessage;
        }
    }

    private ParsedRow parseAndValidateRow(String[] line, HeaderIndices idx, String sellerId, int rowNum, Set<String> seenIsbns, Set<String> seenTitles) {
        String rawIsbn = (idx.isbnIndex != -1 && line.length > idx.isbnIndex) ? line[idx.isbnIndex].trim() : "";
        String title = (idx.titleIndex != -1 && line.length > idx.titleIndex) ? line[idx.titleIndex].trim() : "";

        // Scientific notation ISBN fix
        if (rawIsbn.contains("E+") || rawIsbn.contains("e+")) {
            try {
                rawIsbn = new java.math.BigDecimal(rawIsbn).toPlainString();
            } catch (Exception ignored) {}
        }
        String isbn = rawIsbn.replaceAll("[^0-9X]", "");

        if (title.isEmpty() && isbn.isEmpty()) {
            return new ParsedRow(false, false, null, "Row " + rowNum + " missing title and ISBN");
        }

        // Deduplication inside CSV
        if (!isbn.isEmpty()) {
            if (seenIsbns.contains(isbn)) {
                return new ParsedRow(false, false, null, "Row " + rowNum + " duplicate ISBN in CSV: " + isbn);
            }
            seenIsbns.add(isbn);
        } else if (!title.isEmpty()) {
            String normTitle = title.toLowerCase();
            if (seenTitles.contains(normTitle)) {
                return new ParsedRow(false, false, null, "Row " + rowNum + " duplicate title in CSV: " + title);
            }
            seenTitles.add(normTitle);
        }

        String author = (idx.authorIndex != -1 && line.length > idx.authorIndex) ? line[idx.authorIndex].trim() : "";
        String category = (idx.categoryIndex != -1 && line.length > idx.categoryIndex) ? line[idx.categoryIndex].trim() : "";
        String description = (idx.descriptionIndex != -1 && line.length > idx.descriptionIndex) ? line[idx.descriptionIndex].trim() : "";
        String imageUrl = (idx.imageIndex != -1 && line.length > idx.imageIndex) ? line[idx.imageIndex].trim() : "";

        // Google API details fetch if title is missing
        if (title.isEmpty() && !isbn.isEmpty()) {
            Book fetched = fetchBookDetailsFromGoogle(isbn);
            if (fetched != null) {
                title = fetched.getTitle();
                if (author.isEmpty()) author = fetched.getAuthor();
                if (category.isEmpty()) category = fetched.getCategory();
                if (description.isEmpty()) description = fetched.getDescription();
                if (imageUrl.isEmpty()) imageUrl = fetched.getImageUrl();
            }
        }

        if (title.isEmpty()) {
            return new ParsedRow(false, false, null, "Row " + rowNum + " could not resolve book title");
        }

        if (author.isEmpty()) author = "Unknown Author";
        if (category.isEmpty()) category = "General";
        if (description.isEmpty()) description = "No description available.";
        if (imageUrl.isEmpty() || !imageUrl.startsWith("http")) {
            imageUrl = "https://placehold.co/300x400/e2e8f0/64748b?text=" + title.replace(" ", "+");
        }

        double price = 19.99;
        if (idx.priceIndex != -1 && line.length > idx.priceIndex) {
            try {
                price = Double.parseDouble(line[idx.priceIndex].replaceAll("[^0-9.]", ""));
            } catch (Exception ignored) {}
        }

        int stock = 10;
        if (idx.stockIndex != -1 && line.length > idx.stockIndex) {
            try {
                stock = Integer.parseInt(line[idx.stockIndex].replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {}
        }

        // Matching logic
        Optional<Book> existingOpt = Optional.empty();
        boolean isUpdate = false;

        if (!isbn.isEmpty()) {
            // Case A — Valid ISBN exists: Match ONLY ISBN + sellerId. Never fall back to title.
            existingOpt = bookRepository.findFirstByIsbnAndSellerId(isbn, sellerId);
            isUpdate = existingOpt.isPresent();
        } else if (!title.isEmpty()) {
            // Case B — ISBN missing/blank: Match normalized title + sellerId
            List<Book> titleMatches = bookRepository.findAllByTitleIgnoreCaseAndSellerId(title, sellerId);
            if (titleMatches.size() > 1) {
                return new ParsedRow(false, false, null, "Row " + rowNum + " ambiguous title match: multiple existing books match title '" + title + "'");
            } else if (titleMatches.size() == 1) {
                existingOpt = Optional.of(titleMatches.get(0));
                isUpdate = true;
            }
        }

        Book book;
        if (existingOpt.isPresent()) {
            book = existingOpt.get();
            book.setTitle(title);
            book.setAuthor(author);
            book.setCategory(category);
            book.setDescription(description);
            book.setPrice(price);
            book.setStock(stock);
            book.setImageUrl(imageUrl);
            if (!isbn.isEmpty()) book.setIsbn(isbn);
        } else {
            book = new Book();
            book.setSellerId(sellerId);
            book.setTitle(title);
            book.setAuthor(author);
            book.setCategory(category);
            book.setDescription(description);
            book.setPrice(price);
            book.setStock(stock);
            book.setImageUrl(imageUrl);
            book.setIsbn(isbn);
        }

        return new ParsedRow(true, isUpdate, book, null);
    }

    private Book fetchBookDetailsFromGoogle(String isbn) {
        String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;
        if (googleApiKey != null && !googleApiKey.isEmpty()) {
            url += "&key=" + googleApiKey;
        }

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (root.has("items") && root.get("items").isArray() && root.get("items").size() > 0) {
                JsonNode volumeInfo = root.get("items").get(0).get("volumeInfo");
                Book book = new Book();
                book.setTitle(volumeInfo.path("title").asText("Unknown Title"));
                if (volumeInfo.has("authors") && volumeInfo.get("authors").isArray()) {
                    book.setAuthor(volumeInfo.get("authors").get(0).asText());
                } else {
                    book.setAuthor("Unknown Author");
                }
                book.setDescription(volumeInfo.path("description").asText("No description available."));
                if (volumeInfo.has("categories") && volumeInfo.get("categories").isArray()) {
                    book.setCategory(volumeInfo.get("categories").get(0).asText());
                } else {
                    book.setCategory("General");
                }
                if (volumeInfo.has("imageLinks")) {
                    book.setImageUrl(volumeInfo.get("imageLinks").path("thumbnail").asText("").replace("http://", "https://"));
                }
                return book;
            }
        } catch (Exception e) {
            logger.warn("Google API error for ISBN {}: {}", isbn, e.getMessage());
        }
        return null;
    }
}
