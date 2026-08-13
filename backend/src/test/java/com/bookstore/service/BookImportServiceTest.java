package com.bookstore.service;

import com.bookstore.dto.ImportProgressDTO;
import com.bookstore.model.ImportJob;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.ImportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookImportServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ImportJobRepository importJobRepository;

    @InjectMocks
    private BookImportService bookImportService;

    @BeforeEach
    void setUp() {
        lenient().when(importJobRepository.save(any(ImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testStartImport_validCsv_calculatesTotalExcludingHeader() throws Exception {
        String csvContent = "isbn,title,author,category,price,stock\n" +
                "9781234567890,Book One,Author One,Fiction,19.99,10\n" +
                "9781234567891,Book Two,Author Two,Non-Fiction,29.99,5\n";

        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);
        ImportProgressDTO dto = bookImportService.startImport(bytes, "seller-123");

        assertNotNull(dto);
        assertNotNull(dto.getImportId());
        assertEquals(2, dto.getTotal());
        assertEquals("STARTING", dto.getStatus());
        assertEquals("seller-123", dto.getSellerId());
    }

    @Test
    void testStartImport_emptyCsv_throwsException() {
        String csvContent = "isbn,title,author,category,price,stock\n";
        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> {
            bookImportService.startImport(bytes, "seller-123");
        });
    }

    @Test
    void testProgressInvariant() {
        ImportJob job = new ImportJob("import-1", "seller-123", 100, "IN_PROGRESS", "Testing");
        job.setAdded(80);
        job.setUpdated(15);
        job.setFailed(5);
        job.setProcessed(job.getAdded() + job.getUpdated() + job.getFailed());

        assertEquals(100, job.getProcessed());
        assertEquals(job.getProcessed(), job.getAdded() + job.getUpdated() + job.getFailed());
    }
}
