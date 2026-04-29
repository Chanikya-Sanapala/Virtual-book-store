package com.bookstore.config;

import com.bookstore.model.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, BookRepository bookRepository, PasswordEncoder passwordEncoder) {
        return args -> {
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
        };
    }
}
