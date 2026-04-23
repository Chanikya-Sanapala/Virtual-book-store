package com.bookstore.controller;

import com.bookstore.model.CommunityPost;
import com.bookstore.repository.CommunityPostRepository;
import com.bookstore.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/community")
public class CommunityController {
    @Autowired
    CommunityPostRepository communityPostRepository;

    @GetMapping
    public List<CommunityPost> getAllPosts() {
        return communityPostRepository.findAll();
    }

    @PostMapping
    public CommunityPost createPost(@RequestBody CommunityPost post) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        post.setUserId(userDetails.getId());
        post.setUsername(userDetails.getUsername());
        post.setCreatedAt(new Date());
        return communityPostRepository.save(post);
    }
}
