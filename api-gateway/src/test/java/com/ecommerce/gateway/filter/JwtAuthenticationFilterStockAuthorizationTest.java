package com.ecommerce.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterStockAuthorizationTest {

    private static final String SECRET = "ThisIsA32CharLongGatewayJwtSecret!";

    @Test
    void customerShouldBeForbiddenForStockUpdate() {
        FilterResult result = runFilter("CUSTOMER", HttpMethod.PUT, "/products/3/stock");
        assertThat(result.status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.chainInvoked()).isFalse();
    }

    @Test
    void adminShouldBeAllowedForStockUpdate() {
        FilterResult result = runFilter("ADMIN", HttpMethod.PUT, "/products/3/stock");
        assertThat(result.status()).isNull();
        assertThat(result.chainInvoked()).isTrue();
    }

    @Test
    void sellerShouldBeForbiddenForStockUpdate() {
        FilterResult result = runFilter("SELLER", HttpMethod.PUT, "/products/3/stock");
        assertThat(result.status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.chainInvoked()).isFalse();
    }

    private FilterResult runFilter(String role, HttpMethod method, String path) {
        JwtAuthenticationFilter filterFactory = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filterFactory, "jwtSecret", SECRET);

        String token = createToken(2L, role);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest
                        .method(method, path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build()
        );

        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        GatewayFilterChain chain = new GatewayFilterChain() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange) {
                chainInvoked.set(true);
                return Mono.empty();
            }
        };

        GatewayFilter gatewayFilter = filterFactory.apply(new Object());
        gatewayFilter.filter(exchange, chain).block();
        return new FilterResult(exchange.getResponse().getStatusCode(), chainInvoked.get());
    }

    private String createToken(Long userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(2, ChronoUnit.HOURS)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private record FilterResult(HttpStatusCode status, boolean chainInvoked) {
    }
}
