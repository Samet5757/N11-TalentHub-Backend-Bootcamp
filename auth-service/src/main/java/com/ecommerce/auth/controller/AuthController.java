package com.ecommerce.auth.controller;

import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        User created = authService.register(user);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        String token = authService.login(username, password);
        Map<String, String> response = new HashMap<>();
        if (token != null) {
            response.put("token", token);
            return ResponseEntity.ok(response);
        }
        response.put("error", "Invalid credentials");
        return ResponseEntity.status(401).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<User> me(@RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(authService.getCurrentUser(authorization));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestHeader("Authorization") String authorization) {
        String newToken = authService.refresh(authorization);
        return ResponseEntity.ok(Map.of("token", newToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization);
        return ResponseEntity.noContent().build();
    }
}
