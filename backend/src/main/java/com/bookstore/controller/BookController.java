package com.bookstore.controller;

import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/books")
public class BookController {
    @Autowired
    BookRepository bookRepository;

    @GetMapping
    public List<Book> getAllBooks(@RequestParam(required = false) String title) {
        if (title != null) {
            return bookRepository.findByTitleContainingIgnoreCase(title);
        }
        return bookRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable String id) {
        Optional<Book> book = bookRepository.findById(id);
        return book.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{category}")
    public List<Book> getBooksByCategory(@PathVariable String category) {
        return bookRepository.findByCategory(category);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Book createBook(@RequestBody Book book) {
        return bookRepository.save(book);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Book> updateBook(@PathVariable String id, @RequestBody Book bookDetails) {
        return bookRepository.findById(id).map(book -> {
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteBook(@PathVariable String id) {
        return bookRepository.findById(id).map(book -> {
            bookRepository.delete(book);
            return ResponseEntity.ok().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
