package com.bookstore.controller;

import com.bookstore.dto.*;
import com.bookstore.model.User;
import com.bookstore.model.PasswordResetToken;
import com.bookstore.repository.UserRepository;
import com.bookstore.repository.PasswordResetTokenRepository;
import com.bookstore.security.JwtUtils;
import com.bookstore.security.UserDetailsImpl;
import com.bookstore.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    EmailService emailService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));

        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER");
        user.setRoles(roles);
        userRepository.save(user);

        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
        } catch (Exception e) {
            // Log error but don't fail registration
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            // Return success even if not found to prevent email enumeration
            return ResponseEntity.ok(new MessageResponse("If your email is registered, you will receive a reset link."));
        }

        String tokenStr = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken(tokenStr, user.getEmail(), LocalDateTime.now().plusHours(1));
        
        // Delete any existing tokens for this email
        passwordResetTokenRepository.deleteByEmail(user.getEmail());
        passwordResetTokenRepository.save(token);

        String resetLink = "http://localhost:4200/reset-password?token=" + tokenStr;
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new MessageResponse("Error sending email. Please try again later."));
        }

        return ResponseEntity.ok(new MessageResponse("If your email is registered, you will receive a reset link."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken()).orElse(null);

        if (resetToken == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid or expired token."));
        }

        User user = userRepository.findByEmail(resetToken.getEmail()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found."));
        }

        user.setPassword(encoder.encode(request.getNewPassword()));
        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);

        return ResponseEntity.ok(new MessageResponse("Password successfully reset. You can now login."));
    }

    @PostMapping("/google")
    public ResponseEntity<?> authenticateGoogleUser(@Valid @RequestBody com.bookstore.dto.GoogleLoginRequest googleLoginRequest) {
        try {
            com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier verifier = 
                new com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder(
                    new com.google.api.client.http.javanet.NetHttpTransport(), 
                    new com.google.api.client.json.gson.GsonFactory())
                .setAudience(java.util.Collections.singletonList("115274603227-h9on2aqka1ufhnrnqpo5mf9a08ulbiva.apps.googleusercontent.com"))
                .build();

            com.google.api.client.googleapis.auth.oauth2.GoogleIdToken idToken = verifier.verify(googleLoginRequest.getIdToken());
            if (idToken != null) {
                com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");

                User user = userRepository.findByEmail(email).orElse(null);
                
                if (user == null) {
                    // Register new Google user
                    user = new User();
                    user.setUsername(email.split("@")[0] + "_" + java.util.UUID.randomUUID().toString().substring(0, 5));
                    user.setEmail(email);
                    user.setPassword(encoder.encode(java.util.UUID.randomUUID().toString()));
                    Set<String> roles = new HashSet<>();
                    roles.add("ROLE_USER");
                    user.setRoles(roles);
                    userRepository.save(user);
                    
                    try {
                        emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
                    } catch (Exception e) {
                        System.err.println("Failed to send welcome email to Google user: " + e.getMessage());
                    }
                }

                // Authenticate and generate JWT
                UserDetailsImpl userDetails = UserDetailsImpl.build(user);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                String jwt = jwtUtils.generateJwtToken(authentication);

                List<String> rolesList = userDetails.getAuthorities().stream()
                        .map(item -> item.getAuthority())
                        .collect(Collectors.toList());

                return ResponseEntity.ok(new JwtResponse(jwt,
                        userDetails.getId(),
                        userDetails.getUsername(),
                        userDetails.getEmail(),
                        rolesList));
            } else {
                return ResponseEntity.status(401).body(new MessageResponse("Error: Invalid Google ID token."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new MessageResponse("Error: Authentication failed."));
        }
    }
}
