package com.bookstore.controller;

import com.bookstore.dto.ImportProgressDTO;
import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import com.bookstore.security.UserDetailsImpl;
import com.bookstore.service.BookImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/books")
public class BookController {
    @Autowired
    BookRepository bookRepository;

    @Autowired
    BookImportService bookImportService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP", "timestamp", String.valueOf(System.currentTimeMillis())));
    }

    @GetMapping
    public Page<Book> getAllBooks(
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        System.out.println(">>> BookController: getAllBooks() called - page: " + page + ", size: " + size + ", title: " + title);
        try {
            int pageSize = Math.min(Math.max(size, 1), 50);
            Pageable pageable = PageRequest.of(Math.max(page, 0), pageSize);
            if (title != null && !title.trim().isEmpty()) {
                return bookRepository.findByTitleContainingIgnoreCase(title.trim(), pageable);
            } else {
                return bookRepository.findAll(pageable);
            }
        } catch (Exception e) {
            System.err.println(">>> BookController: Error in getAllBooks: " + e.getMessage());
            return Page.empty();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable String id) {
        System.out.println(">>> BookController: getBookById() called for id: " + id);
        Optional<Book> book = bookRepository.findById(id);
        return book.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{category}")
    public Page<Book> getBooksByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        System.out.println(">>> BookController: getBooksByCategory() called for: " + category + " - page: " + page + ", size: " + size);
        try {
            int pageSize = Math.min(Math.max(size, 1), 50);
            Pageable pageable = PageRequest.of(Math.max(page, 0), pageSize);
            return bookRepository.findByCategory(category, pageable);
        } catch (Exception e) {
            System.err.println(">>> BookController: Error in getBooksByCategory: " + e.getMessage());
            return Page.empty();
        }
    }

    @GetMapping("/seller/{sellerId}")
    public Page<Book> getBooksBySeller(
            @PathVariable String sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        System.out.println(">>> BookController: getBooksBySeller() called for: " + sellerId + " - page: " + page + ", size: " + size);
        try {
            int pageSize = Math.min(Math.max(size, 1), 50);
            Pageable pageable = PageRequest.of(Math.max(page, 0), pageSize);
            return bookRepository.findBySellerId(sellerId, pageable);
        } catch (Exception e) {
            System.err.println(">>> BookController: Error in getBooksBySeller: " + e.getMessage());
            return Page.empty();
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        List<Book> books = bookRepository.findAll();
        List<String> categories = new ArrayList<>();
        for (Book b : books) {
            if (b.getCategory() != null && !b.getCategory().isEmpty() && !categories.contains(b.getCategory())) {
                categories.add(b.getCategory());
            }
        }
        if (categories.isEmpty()) {
            categories.add("Fiction");
            categories.add("Non-Fiction");
            categories.add("Sci-Fi");
            categories.add("Technology");
            categories.add("History");
        }
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> createBook(@RequestBody Book book, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            book.setSellerId(userDetails.getId());
        }
        Book savedBook = bookRepository.save(book);
        return ResponseEntity.ok(savedBook);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> updateBook(@PathVariable String id, @RequestBody Book bookDetails, Authentication authentication) {
        return bookRepository.findById(id).map(book -> {
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                boolean isAdmin = userDetails.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                if (!isAdmin && !book.getSellerId().equals(userDetails.getId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
            book.setTitle(bookDetails.getTitle());
            book.setAuthor(bookDetails.getAuthor());
            book.setDescription(bookDetails.getDescription());
            book.setCategory(bookDetails.getCategory());
            book.setPrice(bookDetails.getPrice());
            book.setStock(bookDetails.getStock());
            if (bookDetails.getImageUrl() != null) book.setImageUrl(bookDetails.getImageUrl());
            if (bookDetails.getIsbn() != null) book.setIsbn(bookDetails.getIsbn());
            
            Book updatedBook = bookRepository.save(book);
            return ResponseEntity.ok(updatedBook);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> deleteBook(@PathVariable String id, Authentication authentication) {
        return bookRepository.findById(id).map(book -> {
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                boolean isAdmin = userDetails.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                if (!isAdmin && !book.getSellerId().equals(userDetails.getId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
            
            bookRepository.delete(book);
            return ResponseEntity.ok().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> importBooks(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            String sellerId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                sellerId = userDetails.getId();
            }
            
            if (sellerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            byte[] fileBytes = file.getBytes();
            ImportProgressDTO initialProgress = bookImportService.startImport(fileBytes, sellerId);
            
            bookImportService.processImportAsync(initialProgress.getImportId(), fileBytes, sellerId);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(initialProgress);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            System.err.println(">>> BookController: Error starting import: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Error starting import: " + e.getMessage()));
        }
    }

    @GetMapping("/import/progress/{importId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> getImportProgress(@PathVariable("importId") String importId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        try {
            ImportProgressDTO progress = bookImportService.getImportProgress(importId, userDetails.getId(), isAdmin);
            if (progress == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(progress);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/import/active")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> getActiveImport(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        ImportProgressDTO activeImport = bookImportService.getActiveImportForSeller(userDetails.getId());
        if (activeImport == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(activeImport);
    }
}
