package com.bookstore.config;

import com.bookstore.model.User;
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
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Create default admin if not exists
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User();
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
        };
    }
}
