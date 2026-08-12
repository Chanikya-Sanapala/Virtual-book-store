package com.bookstore.controller;

import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import com.bookstore.security.UserDetailsImpl;
import com.bookstore.service.BookImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public List<String> getAllCategories() {
        System.out.println(">>> BookController: getAllCategories() called");
        try {
            List<String> categories = List.of("Fiction", "Non-Fiction", "Science", "Technology", "Biography", "Self-Help");
            System.out.println(">>> BookController: returning " + categories.size() + " categories");
            return categories;
        } catch (Exception e) {
            System.err.println(">>> BookController: Error in getAllCategories: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Book createBook(@RequestBody Book book, Authentication authentication) {
        System.out.println(">>> BookController: createBook() called for title: " + book.getTitle());
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            book.setSellerId(userDetails.getId());
        }
        return bookRepository.save(book);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Book> updateBook(@PathVariable String id, @RequestBody Book bookDetails, Authentication authentication) {
        System.out.println(">>> BookController: updateBook() called for id: " + id);
        return bookRepository.findById(id).map(book -> {
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                
                String sellerId = book.getSellerId();
                if (sellerId != null && !sellerId.equals(userDetails.getId()) && !isAdmin) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).<Book>build();
                }
            } else {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).<Book>build();
            }

            book.setTitle(bookDetails.getTitle());
            book.setAuthor(bookDetails.getAuthor());
            book.setDescription(bookDetails.getDescription());
            book.setCategory(bookDetails.getCategory());
            book.setPrice(bookDetails.getPrice());
            book.setStock(bookDetails.getStock());
            book.setImageUrl(bookDetails.getImageUrl());
            return ResponseEntity.ok(bookRepository.save(book));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> deleteBook(@PathVariable String id, Authentication authentication) {
        System.out.println(">>> BookController: deleteBook() called for id: " + id);
        return bookRepository.findById(id).map(book -> {
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                
                String sellerId = book.getSellerId();
                if (sellerId != null && !sellerId.equals(userDetails.getId()) && !isAdmin) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
                }
            } else {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
            }
            
            bookRepository.delete(book);
            return ResponseEntity.ok().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> importBooks(@RequestParam("file") MultipartFile file, Authentication authentication) {
        System.out.println(">>> BookController: importBooks() called");
        try {
            String sellerId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                sellerId = userDetails.getId();
            }
            
            if (sellerId == null) {
                return ResponseEntity.status(401).body("User not authenticated");
            }

            int count = bookImportService.importBooksFromCsv(file, sellerId);
            return ResponseEntity.ok("Successfully imported " + count + " books.");
        } catch (Exception e) {
            System.err.println(">>> BookController: Error in importBooks: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Error importing books: " + e.getMessage());
        }
    }

    @GetMapping("/import/progress")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<String> getImportProgress(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String progress = bookImportService.getImportProgress(userDetails.getId());
            return ResponseEntity.ok(progress);
        }
        return ResponseEntity.status(401).body("");
    }
}
