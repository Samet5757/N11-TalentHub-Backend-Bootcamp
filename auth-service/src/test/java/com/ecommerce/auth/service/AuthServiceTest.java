package com.ecommerce.auth.service;

import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.entity.UserRole;
import com.ecommerce.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
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
        User request = new User("customer-x", "pass123", UserRole.CUSTOMER);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.register(request);

        assertThat(saved.getPassword()).isNotEqualTo("pass123");
        assertThat(passwordEncoder.matches("pass123", saved.getPassword())).isTrue();
    }

    @Test
    void login_shouldUsePasswordEncoderMatches() {
        User dbUser = new User("customer-y", passwordEncoder.encode("pass123"), UserRole.CUSTOMER);
        dbUser.setId(10L);
        when(userRepository.findByUsername("customer-y")).thenReturn(dbUser);

        String okToken = authService.login("customer-y", "pass123");
        String failToken = authService.login("customer-y", "wrong");

        assertThat(okToken).isNotBlank();
        assertThat(failToken).isNull();
    }
}
