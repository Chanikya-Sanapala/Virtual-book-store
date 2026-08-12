package com.bookstore.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "books")
public class Book {
    @Id
    private String id;
    
    @Indexed
    private String title;
    
    private String author;
    private String description;
    
    @Indexed
    private String category;
    
    private double price;
    private int stock;
    
    @Indexed
    private String sellerId;
    
    private String imageUrl;
    private String isbn;

    // Default constructor
    public Book() {}

    // All-args constructor
    public Book(String id, String title, String author, String description, String category, double price, int stock, String sellerId, String imageUrl) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.description = description;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.sellerId = sellerId;
        this.imageUrl = imageUrl;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
}
