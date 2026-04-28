package com.ecommerce.auth.service;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;
@Service
public class AuthService {
    private final UserRepository userRepository;
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    public User register(User user) {
        return userRepository.save(user);
    }
    public String login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return "token-" + UUID.randomUUID().toString();
        }
        return null;
    }
}
