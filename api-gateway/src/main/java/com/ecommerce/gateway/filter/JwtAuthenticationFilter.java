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
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<Object> {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> PRIVILEGED_ROLES = Set.of("ADMIN", "SELLER");
    private static final Pattern CART_CUSTOMER_PATH = Pattern.compile("^/carts/customer/(\\d+)$");
    private static final Pattern ORDER_CUSTOMER_PATH = Pattern.compile("^/orders/customer/(\\d+)$");
    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/auth/",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars/"
    );

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

                if (!isAuthorized(exchange, userId, role)) {
                    return forbidden(exchange);
                }

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

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    private boolean isAuthorized(ServerWebExchange exchange, String userId, String role) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        boolean privileged = PRIVILEGED_ROLES.contains(normalizedRole);
        boolean customer = "CUSTOMER".equals(normalizedRole);

        if (path.startsWith("/products")) {
            return method == HttpMethod.GET || privileged;
        }

        if (path.startsWith("/sellers")) {
            return privileged;
        }

        if (path.startsWith("/campaigns")) {
            return privileged;
        }

        if (path.startsWith("/payments")) {
            if (path.equals("/payments") && method == HttpMethod.GET) {
                return privileged;
            }
            if (path.startsWith("/payments/order/") && method == HttpMethod.GET) {
                return customer || privileged;
            }
            return customer || privileged;
        }

        if (path.startsWith("/carts")) {
            if (path.equals("/carts") && method == HttpMethod.GET) {
                return privileged;
            }
            if (path.equals("/carts") && method == HttpMethod.POST) {
                return customer;
            }

            Matcher cartCustomerMatcher = CART_CUSTOMER_PATH.matcher(path);
            if (cartCustomerMatcher.matches()) {
                String requestedCustomerId = cartCustomerMatcher.group(1);
                return privileged || (customer && requestedCustomerId.equals(userId));
            }
            return customer || privileged;
        }

        if (path.startsWith("/orders")) {
            if (path.equals("/orders") && method == HttpMethod.GET) {
                return privileged;
            }
            if (path.equals("/orders") && method == HttpMethod.POST) {
                return customer;
            }

            Matcher orderCustomerMatcher = ORDER_CUSTOMER_PATH.matcher(path);
            if (orderCustomerMatcher.matches()) {
                String requestedCustomerId = orderCustomerMatcher.group(1);
                return privileged || (customer && requestedCustomerId.equals(userId));
            }

            if (path.matches("^/orders/\\d+/status$") && method == HttpMethod.PUT) {
                return privileged;
            }
            return customer || privileged;
        }

        return true;
    }
}
