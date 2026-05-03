package com.ecommerce.auth.service;

import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.entity.UserRole;
import com.ecommerce.auth.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(authService, "jwtSecret", "01234567890123456789012345678901");
    }

    @Test
    void register_shouldHashPasswordBeforePersist() {
        User request = new User("customer-x", "pass123", "customer-x@example.local", UserRole.CUSTOMER);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.register(request);

        assertThat(saved.getPassword()).isNotEqualTo("pass123");
        assertThat(passwordEncoder.matches("pass123", saved.getPassword())).isTrue();
    }

    @Test
    void login_shouldUsePasswordEncoderMatches() {
        User dbUser = new User("customer-y", passwordEncoder.encode("pass123"), "customer-y@example.local", UserRole.CUSTOMER);
        dbUser.setId(10L);
        when(userRepository.findByUsernameOrEmail("customer-y", "customer-y")).thenReturn(dbUser);

        String okToken = authService.login("customer-y", "pass123");
        String failToken = authService.login("customer-y", "wrong");

        assertThat(okToken).isNotBlank();
        assertThat(failToken).isNull();
    }

    @Test
    void register_whenRoleMissing_shouldDefaultCustomer() {
        User request = new User("new-user", "pass123", "new-user@example.local", null);
        when(userRepository.findByEmail("new-user@example.local")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.register(request);

        assertThat(saved.getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void register_whenEmailMissing_shouldThrowBadRequest() {
        User request = new User("no-email", "pass123", null, UserRole.CUSTOMER);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(request));
        assertThat(ex.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void register_whenEmailAlreadyExists_shouldThrowConflict() {
        User request = new User("dupe", "pass123", "dupe@example.local", UserRole.CUSTOMER);
        when(userRepository.findByEmail("dupe@example.local")).thenReturn(Optional.of(new User()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(request));
        assertThat(ex.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void refresh_afterLogout_shouldThrowRevoked() {
        User dbUser = new User("customer-z", passwordEncoder.encode("pass123"), "customer-z@example.local", UserRole.CUSTOMER);
        dbUser.setId(22L);
        when(userRepository.findByUsernameOrEmail("customer-z", "customer-z")).thenReturn(dbUser);

        String token = authService.login("customer-z", "pass123");
        authService.logout("Bearer " + token);

        assertThrows(IllegalArgumentException.class, () -> authService.refresh("Bearer " + token));
    }

    @Test
    void getCurrentUser_shouldResolveUserFromToken() {
        User dbUser = new User("customer-a", passwordEncoder.encode("pass123"), "customer-a@example.local", UserRole.CUSTOMER);
        dbUser.setId(35L);
        when(userRepository.findByUsernameOrEmail("customer-a", "customer-a")).thenReturn(dbUser);
        when(userRepository.findById(35L)).thenReturn(Optional.of(dbUser));

        String token = authService.login("customer-a", "pass123");
        User resolved = authService.getCurrentUser("Bearer " + token);

        assertThat(resolved.getId()).isEqualTo(35L);
        assertThat(resolved.getUsername()).isEqualTo("customer-a");
    }
}
