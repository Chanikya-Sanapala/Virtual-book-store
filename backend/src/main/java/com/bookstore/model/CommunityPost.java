package com.bookstore.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "community_posts")
public class CommunityPost {
    @Id
    private String id;
    private String userId;
    private String username;
    private String title;
    private String content;
    private Date createdAt;
}
