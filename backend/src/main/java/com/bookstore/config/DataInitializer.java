package com.bookstore.config;

import com.bookstore.model.*;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, BookRepository bookRepository, PasswordEncoder passwordEncoder, MongoTemplate mongoTemplate) {
        return args -> {
            try {
                // Ensure explicit production database indexes
                mongoTemplate.indexOps(User.class).ensureIndex(new Index().on("email", Sort.Direction.ASC).unique());
                mongoTemplate.indexOps(User.class).ensureIndex(new Index().on("username", Sort.Direction.ASC).unique());
                mongoTemplate.indexOps(Book.class).ensureIndex(new Index().on("category", Sort.Direction.ASC));
                mongoTemplate.indexOps(Book.class).ensureIndex(new Index().on("sellerId", Sort.Direction.ASC));
                mongoTemplate.indexOps(Book.class).ensureIndex(new Index().on("title", Sort.Direction.ASC));
                mongoTemplate.indexOps(Order.class).ensureIndex(new Index().on("userId", Sort.Direction.ASC));
                mongoTemplate.indexOps(Review.class).ensureIndex(new Index().on("bookId", Sort.Direction.ASC));
                mongoTemplate.indexOps(PasswordResetToken.class).ensureIndex(new Index().on("token", Sort.Direction.ASC));
                mongoTemplate.indexOps(PasswordResetToken.class).ensureIndex(new Index().on("email", Sort.Direction.ASC));
                System.out.println(">>> DataInitializer: Explicit MongoDB indexes ensured successfully.");
            } catch (Exception e) {
                System.err.println(">>> DataInitializer: Index creation warning: " + e.getMessage());
            }
            // Clear existing books as requested (Commented out so we don't wipe your Kaggle books on next restart!)
            // bookRepository.deleteAll();
            // System.out.println(">>> DataInitializer: All books cleared from database.");

            // Create default admin if not exists
            User admin = userRepository.findByUsername("admin").orElse(null);
            if (admin == null) {
                admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@leafybooks.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                
                Set<String> roles = new HashSet<>();
                roles.add("ROLE_ADMIN");
                roles.add("ROLE_USER");
                admin.setRoles(roles);
                
                userRepository.save(admin);
                System.out.println("Default Admin created: admin / admin123");
            }
            
            /* 
            // Re-adding sample books for visualization
            if (bookRepository.count() == 0) {
                com.bookstore.model.Book b1 = new com.bookstore.model.Book();
                b1.setTitle("The Great Gatsby");
                b1.setAuthor("F. Scott Fitzgerald");
                b1.setPrice(15.99);
                b1.setCategory("Fiction");
                b1.setStock(10);
                b1.setImageUrl("https://images-na.ssl-images-amazon.com/images/I/81af+MCATTL.jpg");
                
                com.bookstore.model.Book b2 = new com.bookstore.model.Book();
                b2.setTitle("Clean Code");
                b2.setAuthor("Robert C. Martin");
                b2.setPrice(34.99);
                b2.setCategory("Technology");
                b2.setStock(5);
                b2.setImageUrl("https://images-na.ssl-images-amazon.com/images/I/41xShlnTZTL._SX376_BO1,204,203,200_.jpg");
                
                bookRepository.save(b1);
                bookRepository.save(b2);
                System.out.println("Sample books created.");
            }
            */
        };
    }
}
