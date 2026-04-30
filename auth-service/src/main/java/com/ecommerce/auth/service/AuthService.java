package com.ecommerce.auth.service;

import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(User user) {
        log.info("Registering user username={}", user == null ? null : user.getUsername());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public String login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            log.info("Login successful for username={}", username);
            return generateToken(user);
        }
        log.warn("Login failed for username={}", username);
        return null;
    }

    public User getCurrentUser(String bearerToken) {
        Claims claims = parseClaims(extractToken(bearerToken));
        Long userId = Long.parseLong(claims.get("userId").toString());
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public String refresh(String bearerToken) {
        String token = extractToken(bearerToken);
        if (revokedTokens.contains(token)) {
            throw new IllegalArgumentException("Token revoked");
        }
        Claims claims = parseClaims(token);
        Long userId = Long.parseLong(claims.get("userId").toString());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return generateToken(user);
    }

    public void logout(String bearerToken) {
        log.info("Logging out token");
        revokedTokens.add(extractToken(bearerToken));
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

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Jws<Claims> jws = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
        return jws.getBody();
    }

    private String extractToken(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header is required");
        }
        return bearerToken.substring(7);
    }
}
