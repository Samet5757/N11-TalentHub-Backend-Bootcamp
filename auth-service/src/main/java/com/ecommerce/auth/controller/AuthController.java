package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.AuthLoginRequest;
import com.ecommerce.auth.dto.AuthTokenResponse;
import com.ecommerce.auth.dto.AuthUserResponse;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.UserContactResponse;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.entity.UserRole;
import com.ecommerce.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User toCreate = new User();
        toCreate.setUsername(request.email());
        toCreate.setPassword(request.password());
        toCreate.setEmail(request.email());
        toCreate.setFirstName(request.firstName());
        toCreate.setLastName(request.lastName());
        toCreate.setRole(UserRole.CUSTOMER);
        User created = authService.register(toCreate);
        return ResponseEntity.ok(toUserResponse(created));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        String token = authService.login(request.username(), request.password());
        if (token != null) {
            return ResponseEntity.ok(new AuthTokenResponse(token));
        }
        return ResponseEntity.status(401).build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> me(@RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(toUserResponse(authService.getCurrentUser(authorization)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(@RequestHeader("Authorization") String authorization) {
        String newToken = authService.refresh(authorization);
        return ResponseEntity.ok(new AuthTokenResponse(newToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/internal/users/{id}/contact")
    public ResponseEntity<UserContactResponse> userContact(@PathVariable Long id) {
        User user = authService.getUserById(id);
        return ResponseEntity.ok(new UserContactResponse(user.getId(), user.getEmail()));
    }

    private AuthUserResponse toUserResponse(User user) {
        return new AuthUserResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
