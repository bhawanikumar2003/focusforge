package com.focusforge.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.focusforge.config.JwtUtil;
import com.focusforge.dto.AuthResponse;
import com.focusforge.exception.CustomException;
import com.focusforge.model.RefreshToken;
import com.focusforge.model.User;
import com.focusforge.repository.RefreshTokenRepository;
import com.focusforge.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // ================= MAKE ADMIN =================
    public String createAdmin(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException("User not found"));

        user.setRole("ADMIN");
        userRepository.save(user);

        return "Admin role assigned";
    }

    // ================= REGISTER =================
    public String registerUser(String name,
                               String email,
                               String password) {

        if (userRepository.findByEmail(email).isPresent()) {
            throw new CustomException("Email already registered");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");

        userRepository.save(user);

        return "User registered successfully";
    }

    // ================= LOGIN =================
    public AuthResponse loginUser(String email,
                                  String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException("User not found"));

        // ===== CHECK IF ACCOUNT LOCKED =====
        if (user.isAccountLocked()) {

            if (user.getLockTime() != null &&
                user.getLockTime()
                    .plusMinutes(10)
                    .isAfter(LocalDateTime.now())) {

                throw new CustomException("Account locked. Try again later.");
            } else {
                // Unlock account after 10 minutes
                user.setAccountLocked(false);
                user.setFailedAttempts(0);
                user.setLockTime(null);
                userRepository.save(user);
            }
        }

        // ===== CHECK PASSWORD =====
        if (!passwordEncoder.matches(password, user.getPassword())) {

            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);

            if (attempts >= 5) {
                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now());
            }

            userRepository.save(user);

            throw new CustomException("Invalid credentials");
        }

        // ===== SUCCESS LOGIN =====
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        user.setLockTime(null);
        userRepository.save(user);

        // Generate Access Token
        String accessToken =
                jwtUtil.generateToken(user.getEmail(), user.getRole());

        // Generate Refresh Token
        String refreshTokenValue =
                UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken(
                refreshTokenValue,
                LocalDateTime.now().plusDays(7),
                user
        );

        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, refreshTokenValue);
    }
}