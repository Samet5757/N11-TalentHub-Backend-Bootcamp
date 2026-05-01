package com.ecommerce.auth.config;

import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.entity.UserRole;
import com.ecommerce.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminUserSeeder {
    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    @Bean
    CommandLineRunner seedDefaultAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@n11.com";
            String adminPassword = "admin123";

            User admin = userRepository.findByEmail(adminEmail).orElse(null);
            if (admin == null) {
                User created = new User("admin@n11.com", passwordEncoder.encode(adminPassword), adminEmail, UserRole.ADMIN);
                created.setFirstName("Platform");
                created.setLastName("Admin");
                userRepository.save(created);
                log.info("Default admin seeded with email={}", adminEmail);
                return;
            }

            boolean changed = false;
            if (admin.getRole() != UserRole.ADMIN) {
                admin.setRole(UserRole.ADMIN);
                changed = true;
            }
            if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(adminPassword));
                changed = true;
            }
            if (admin.getUsername() == null || admin.getUsername().isBlank()) {
                admin.setUsername(adminEmail);
                changed = true;
            }
            if (admin.getFirstName() == null || admin.getFirstName().isBlank()) {
                admin.setFirstName("Platform");
                changed = true;
            }
            if (admin.getLastName() == null || admin.getLastName().isBlank()) {
                admin.setLastName("Admin");
                changed = true;
            }

            if (changed) {
                userRepository.save(admin);
                log.info("Default admin account refreshed for email={}", adminEmail);
            }
        };
    }
}
