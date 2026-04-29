package com.bookstore.controller;

import com.bookstore.model.Review;
import com.bookstore.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @GetMapping("/book/{bookId}")
    public List<Review> getReviewsByBook(@PathVariable String bookId) {
        return reviewRepository.findByBookId(bookId);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Review addReview(@RequestBody Review review) {
        return reviewRepository.save(review);
    }
}
