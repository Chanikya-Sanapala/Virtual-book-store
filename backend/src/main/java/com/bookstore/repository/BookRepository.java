package com.bookstore.repository;

import com.bookstore.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface BookRepository extends MongoRepository<Book, String> {
    List<Book> findByTitleContainingIgnoreCase(String title);
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    
    List<Book> findByAuthorContainingIgnoreCase(String author);
    Page<Book> findByAuthorContainingIgnoreCase(String author, Pageable pageable);
    
    List<Book> findByCategory(String category);
    Page<Book> findByCategory(String category, Pageable pageable);
    
    List<Book> findBySellerId(String sellerId);
    Page<Book> findBySellerId(String sellerId, Pageable pageable);
    
    Optional<Book> findByIsbnAndSellerId(String isbn, String sellerId);
    Optional<Book> findByTitleIgnoreCaseAndSellerId(String title, String sellerId);
    List<Book> findAllByTitleIgnoreCaseAndSellerId(String title, String sellerId);

    List<Book> findTop20ByOrderByIdDesc();
}
