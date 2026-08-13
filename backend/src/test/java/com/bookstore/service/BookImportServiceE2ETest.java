package com.bookstore.service;

import com.bookstore.dto.ImportProgressDTO;
import com.bookstore.model.Book;
import com.bookstore.model.ImportJob;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.ImportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookImportServiceE2ETest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ImportJobRepository importJobRepository;

    @InjectMocks
    private BookImportService bookImportService;

    private Map<String, ImportJob> jobStore = new HashMap<>();
    private Map<String, Book> bookStore = new HashMap<>();

    @BeforeEach
    void setUp() {
        jobStore.clear();
        bookStore.clear();

        lenient().when(importJobRepository.save(any(ImportJob.class))).thenAnswer(invocation -> {
            ImportJob job = invocation.getArgument(0);
            jobStore.put(job.getId(), job);
            return job;
        });

        lenient().when(importJobRepository.findById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return Optional.ofNullable(jobStore.get(id));
        });

        lenient().when(importJobRepository.findByStatusIn(anyList())).thenAnswer(invocation -> {
            List<String> statuses = invocation.getArgument(0);
            return jobStore.values().stream()
                    .filter(j -> statuses.contains(j.getStatus()))
                    .collect(Collectors.toList());
        });

        lenient().when(importJobRepository.findFirstBySellerIdAndStatusInOrderByStartedAtDesc(anyString(), anyList())).thenAnswer(invocation -> {
            String sellerId = invocation.getArgument(0);
            List<String> statuses = invocation.getArgument(1);
            return jobStore.values().stream()
                    .filter(j -> sellerId.equals(j.getSellerId()) && statuses.contains(j.getStatus()))
                    .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
                    .findFirst();
        });

        lenient().when(bookRepository.saveAll(anyIterable())).thenAnswer(invocation -> {
            Iterable<Book> books = invocation.getArgument(0);
            List<Book> list = new ArrayList<>();
            for (Book b : books) {
                if (b.getId() == null) b.setId(UUID.randomUUID().toString());
                bookStore.put(b.getId(), b);
                list.add(b);
            }
            return list;
        });

        lenient().when(bookRepository.findByIsbnAndSellerId(anyString(), anyString())).thenAnswer(invocation -> {
            String isbn = invocation.getArgument(0);
            String sellerId = invocation.getArgument(1);
            return bookStore.values().stream()
                    .filter(b -> sellerId.equals(b.getSellerId()) && isbn.equals(b.getIsbn()))
                    .findFirst();
        });

        lenient().when(bookRepository.findFirstByIsbnAndSellerId(anyString(), anyString())).thenAnswer(invocation -> {
            String isbn = invocation.getArgument(0);
            String sellerId = invocation.getArgument(1);
            return bookStore.values().stream()
                    .filter(b -> sellerId.equals(b.getSellerId()) && isbn.equals(b.getIsbn()))
                    .findFirst();
        });

        lenient().when(bookRepository.findByTitleIgnoreCaseAndSellerId(anyString(), anyString())).thenAnswer(invocation -> {
            String title = invocation.getArgument(0);
            String sellerId = invocation.getArgument(1);
            return bookStore.values().stream()
                    .filter(b -> sellerId.equals(b.getSellerId()) && title.equalsIgnoreCase(b.getTitle()))
                    .findFirst();
        });

        lenient().when(bookRepository.findAllByTitleIgnoreCaseAndSellerId(anyString(), anyString())).thenAnswer(invocation -> {
            String title = invocation.getArgument(0);
            String sellerId = invocation.getArgument(1);
            return bookStore.values().stream()
                    .filter(b -> sellerId.equals(b.getSellerId()) && title.equalsIgnoreCase(b.getTitle()))
                    .collect(Collectors.toList());
        });
    }

    /**
     * Test 1 — Real 500-book CSV
     * Expected: total=500, processed=500, added=500, updated=0, failed=0
     * Verifies both Metamorphoses books exist as separate records with different ISBNs.
     */
    @Test
    void test1_real500BookCsv_allAdded() throws Exception {
        Path csvPath = Path.of("d:/Virtual-book-store-main/Virtual-book-store-main/500_beautiful_books.csv");
        assertTrue(Files.exists(csvPath), "500_beautiful_books.csv must exist");

        byte[] bytes = Files.readAllBytes(csvPath);
        ImportProgressDTO initial = bookImportService.startImport(bytes, "seller-500-test");

        assertNotNull(initial);
        assertEquals(500, initial.getTotal());
        assertEquals("STARTING", initial.getStatus());

        bookImportService.processImportAsync(initial.getImportId(), bytes, "seller-500-test");

        ImportProgressDTO finalProgress = bookImportService.getImportProgress(initial.getImportId(), "seller-500-test", false);
        assertNotNull(finalProgress);
        assertEquals(500, finalProgress.getTotal());
        assertEquals(500, finalProgress.getProcessed());
        assertEquals(500, finalProgress.getAdded(), "All 500 books must be added as separate records");
        assertEquals(0, finalProgress.getUpdated(), "No updates expected when ISBNs differ");
        assertEquals(0, finalProgress.getFailed(), "No failures expected for valid rows");
        assertEquals(100, finalProgress.getPercentage());
        assertEquals("COMPLETED", finalProgress.getStatus());

        // Invariant check
        assertEquals(500, finalProgress.getAdded() + finalProgress.getUpdated() + finalProgress.getFailed());

        // Verify both Metamorphoses books exist as separate records with distinct ISBNs
        List<Book> metamorphosesBooks = bookStore.values().stream()
                .filter(b -> "Metamorphoses".equalsIgnoreCase(b.getTitle()))
                .collect(Collectors.toList());

        assertEquals(2, metamorphosesBooks.size(), "Both Metamorphoses records must exist separately");
        assertNotEquals(metamorphosesBooks.get(0).getIsbn(), metamorphosesBooks.get(1).getIsbn(), "ISBNs must differ");
    }

    /**
     * Test 2 — Existing ISBN
     * Import book matching existing ISBN + seller → updated++
     */
    @Test
    void test2_existingIsbn_updatesBook() throws Exception {
        Book existing = new Book();
        existing.setId("existing-1");
        existing.setSellerId("seller-2");
        existing.setIsbn("9781234567890");
        existing.setTitle("Original Title");
        existing.setPrice(10.0);
        bookStore.put(existing.getId(), existing);

        String csv = "isbn,title,author,category,price,stock\n9781234567890,Updated Title,Author,Fiction,25.00,20\n";
        byte[] bytes = csv.getBytes();

        ImportProgressDTO initial = bookImportService.startImport(bytes, "seller-2");
        bookImportService.processImportAsync(initial.getImportId(), bytes, "seller-2");

        ImportProgressDTO finalProgress = bookImportService.getImportProgress(initial.getImportId(), "seller-2", false);
        assertEquals(1, finalProgress.getTotal());
        assertEquals(1, finalProgress.getProcessed());
        assertEquals(0, finalProgress.getAdded());
        assertEquals(1, finalProgress.getUpdated());
        assertEquals(0, finalProgress.getFailed());

        assertEquals("Updated Title", bookStore.get("existing-1").getTitle());
    }

    /**
     * Test 3 — Same title, different ISBN
     * Importing 2 rows with same title but different ISBNs produces 2 separate books (added=2, updated=0)
     */
    @Test
    void test3_sameTitleDifferentIsbn_createsTwoBooks() throws Exception {
        String csv = "isbn,title,author,category,price,stock\n" +
                "1111111111111,Metamorphoses,Ovid,Fiction,15.00,10\n" +
                "2222222222222,Metamorphoses,Lucius Apuleius,Fiction,18.00,10\n";
        byte[] bytes = csv.getBytes();

        ImportProgressDTO initial = bookImportService.startImport(bytes, "seller-3");
        bookImportService.processImportAsync(initial.getImportId(), bytes, "seller-3");

        ImportProgressDTO finalProgress = bookImportService.getImportProgress(initial.getImportId(), "seller-3", false);
        assertEquals(2, finalProgress.getTotal());
        assertEquals(2, finalProgress.getProcessed());
        assertEquals(2, finalProgress.getAdded());
        assertEquals(0, finalProgress.getUpdated());
        assertEquals(0, finalProgress.getFailed());

        assertEquals(2, bookStore.size());
    }

    /**
     * Test 4 — Missing ISBN
     * Blank ISBN in CSV row + matching title in DB → updated=1
     */
    @Test
    void test4_missingIsbn_titleMatchUpdatesBook() throws Exception {
        Book existing = new Book();
        existing.setId("existing-4");
        existing.setSellerId("seller-4");
        existing.setIsbn("9789999999999");
        existing.setTitle("Unique Title");
        existing.setPrice(12.0);
        bookStore.put(existing.getId(), existing);

        String csv = "isbn,title,author,category,price,stock\n,Unique Title,Updated Author,Fiction,20.00,15\n";
        byte[] bytes = csv.getBytes();

        ImportProgressDTO initial = bookImportService.startImport(bytes, "seller-4");
        bookImportService.processImportAsync(initial.getImportId(), bytes, "seller-4");

        ImportProgressDTO finalProgress = bookImportService.getImportProgress(initial.getImportId(), "seller-4", false);
        assertEquals(1, finalProgress.getTotal());
        assertEquals(1, finalProgress.getProcessed());
        assertEquals(0, finalProgress.getAdded());
        assertEquals(1, finalProgress.getUpdated());
        assertEquals(0, finalProgress.getFailed());

        assertEquals("Updated Author", bookStore.get("existing-4").getAuthor());
    }

    /**
     * Test 5 — Ambiguous title without ISBN
     * Blank ISBN in CSV row + 2 existing books with same title → failed=1
     */
    @Test
    void test5_ambiguousTitleWithoutIsbn_fails() throws Exception {
        Book existing1 = new Book();
        existing1.setId("existing-5a");
        existing1.setSellerId("seller-5");
        existing1.setIsbn("11111");
        existing1.setTitle("Ambiguous Title");
        bookStore.put(existing1.getId(), existing1);

        Book existing2 = new Book();
        existing2.setId("existing-5b");
        existing2.setSellerId("seller-5");
        existing2.setIsbn("22222");
        existing2.setTitle("Ambiguous Title");
        bookStore.put(existing2.getId(), existing2);

        String csv = "isbn,title,author,category,price,stock\n,Ambiguous Title,New Author,Fiction,20.00,15\n";
        byte[] bytes = csv.getBytes();

        ImportProgressDTO initial = bookImportService.startImport(bytes, "seller-5");
        bookImportService.processImportAsync(initial.getImportId(), bytes, "seller-5");

        ImportProgressDTO finalProgress = bookImportService.getImportProgress(initial.getImportId(), "seller-5", false);
        assertEquals(1, finalProgress.getTotal());
        assertEquals(1, finalProgress.getProcessed());
        assertEquals(0, finalProgress.getAdded());
        assertEquals(0, finalProgress.getUpdated());
        assertEquals(1, finalProgress.getFailed());
    }

    /**
     * Test 6 — Stale import recovery
     * Interrupted IN_PROGRESS job with old updatedAt -> marked FAILED on reconciliation. Recent job remains IN_PROGRESS.
     */
    @Test
    void test6_staleImportRecovery() {
        ImportJob staleJob = new ImportJob("job-stale", "seller-6", 100, "IN_PROGRESS", "Processing...");
        staleJob.setStartedAt(Instant.now().minusSeconds(600));
        staleJob.setUpdatedAt(Instant.now().minusSeconds(600));
        jobStore.put(staleJob.getId(), staleJob);

        ImportJob recentJob = new ImportJob("job-recent", "seller-6", 100, "IN_PROGRESS", "Processing...");
        recentJob.setStartedAt(Instant.now());
        recentJob.setUpdatedAt(Instant.now());
        jobStore.put(recentJob.getId(), recentJob);

        bookImportService.reconcileStaleJobs();

        assertEquals("FAILED", jobStore.get("job-stale").getStatus());
        assertTrue(jobStore.get("job-stale").getMessage().contains("interrupted"));

        assertEquals("IN_PROGRESS", jobStore.get("job-recent").getStatus());
    }

    /**
     * Test 7 — Active import security
     * Active import for Seller A is returned to Seller A, but NOT Seller B. Unauthorized status query throws SecurityException.
     */
    @Test
    void test7_activeImportSecurity() throws Exception {
        ImportJob jobA = new ImportJob("job-seller-a", "seller-A", 100, "IN_PROGRESS", "Processing...");
        jobStore.put(jobA.getId(), jobA);

        ImportProgressDTO activeA = bookImportService.getActiveImportForSeller("seller-A");
        assertNotNull(activeA);
        assertEquals("job-seller-a", activeA.getImportId());

        ImportProgressDTO activeB = bookImportService.getActiveImportForSeller("seller-B");
        assertNull(activeB, "Seller B must NOT receive Seller A's active import job");

        assertThrows(SecurityException.class, () -> {
            bookImportService.getImportProgress("job-seller-a", "seller-B", false);
        });
    }
}
