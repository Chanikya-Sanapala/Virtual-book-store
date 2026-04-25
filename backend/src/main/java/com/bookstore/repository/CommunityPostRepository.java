package com.bookstore.repository;

import com.bookstore.model.CommunityPost;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommunityPostRepository extends MongoRepository<CommunityPost, String> {
}
