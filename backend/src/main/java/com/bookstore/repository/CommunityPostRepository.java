package com.bookstore.repository;

import com.bookstore.model.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, String> {
}
