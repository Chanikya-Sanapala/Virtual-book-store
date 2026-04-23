package com.bookstore.controller;

import com.bookstore.model.Order;
import com.bookstore.repository.OrderRepository;
import com.bookstore.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    OrderRepository orderRepository;

    @PostMapping
    public Order placeOrder(@RequestBody Order order) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        order.setUserId(userDetails.getId());
        order.setOrderDate(new Date());
        order.setStatus("COMPLETED"); // For demo purposes, auto-complete
        return orderRepository.save(order);
    }

    @GetMapping("/my-orders")
    public List<Order> getMyOrders() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return orderRepository.findByUserId(userDetails.getId());
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
