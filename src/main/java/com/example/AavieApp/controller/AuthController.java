package com.example.AavieApp.controller;

import com.example.AavieApp.service.AuthService;
import com.example.AavieApp.service.AuthService.AuthResponse;
import com.example.AavieApp.service.AuthService.LoginRequest;
import com.example.AavieApp.service.AuthService.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            AuthResponse res = service.register(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(res);
        } catch (IllegalStateException e) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Registration failed. Please try again."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            AuthResponse res = service.login(req);
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Login failed. Please try again."));
        }
    }

    @PostMapping("/login/admin")
    public ResponseEntity<?> adminLogin(@RequestBody LoginRequest req) {
        try {
            AuthResponse res = service.adminLogin(req);
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Login failed. Please try again."));
        }
    }
    
    @PostMapping("/login/order-admin")
    public ResponseEntity<?> orderAdminLogin(@RequestBody LoginRequest req) {
        try {
            AuthResponse res = service.orderAdminLogin(req);
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Login failed. Please try again."));
        }
    }
}