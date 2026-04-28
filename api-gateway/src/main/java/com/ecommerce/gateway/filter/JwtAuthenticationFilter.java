package com.ecommerce.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<Object> {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> EXCLUDED_PREFIXES = List.of("/auth/");

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            if (isExcludedPath(exchange)) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                return unauthorized(exchange);
            }

            String token = authHeader.substring(BEARER_PREFIX.length());
            try {
                Claims claims = parseToken(token).getBody();
                String userId = extractUserId(claims);
                String role = extractRole(claims);

                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(builder -> builder
                                .header("X-User-Id", userId)
                                .header("X-User-Role", role))
                        .build();

                return chain.filter(mutatedExchange);
            } catch (JwtException | IllegalArgumentException exception) {
                return unauthorized(exchange);
            }
        };
    }

    private boolean isExcludedPath(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Jws<Claims> parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    private String extractUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId != null) {
            return String.valueOf(userId);
        }
        if (claims.getSubject() != null && !claims.getSubject().isBlank()) {
            return claims.getSubject();
        }
        throw new IllegalArgumentException("Missing userId claim");
    }

    private String extractRole(Claims claims) {
        Object role = claims.get("role");
        if (role != null && !String.valueOf(role).isBlank()) {
            return String.valueOf(role);
        }
        throw new IllegalArgumentException("Missing role claim");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
