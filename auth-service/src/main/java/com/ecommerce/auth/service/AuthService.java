package com.ecommerce.auth.service;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class AuthService {
    private final UserRepository userRepository;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    public User register(User user) {
        return userRepository.save(user);
    }
    public String login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return generateToken(user);
        }
        return null;
    }

    private String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(24, ChronoUnit.HOURS);
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
