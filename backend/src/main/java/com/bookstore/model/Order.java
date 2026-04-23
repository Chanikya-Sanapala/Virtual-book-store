package com.bookstore.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String userId;
    @ElementCollection
    private List<OrderItem> items;
    private double totalAmount;
    private String status; // PENDING, COMPLETED, CANCELLED
    private Date orderDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class OrderItem {
        private String bookId;
        private String title;
        private int quantity;
        private double price;
    }
}
