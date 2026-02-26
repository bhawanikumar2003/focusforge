package com.focusforge.controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.focusforge.config.JwtUtil;
import com.focusforge.dto.AuthResponse;
import com.focusforge.dto.LoginRequest;
import com.focusforge.dto.RegisterRequest;
import com.focusforge.dto.UserProfileDTO;
import com.focusforge.exception.CustomException;
import com.focusforge.model.RefreshToken;
import com.focusforge.model.User;
import com.focusforge.repository.RefreshTokenRepository;
import com.focusforge.repository.UserRepository;
import com.focusforge.service.UserService;

import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService,
                          UserRepository userRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          JwtUtil jwtUtil,
                          PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    // ================= REGISTER =================
    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterRequest request) {

        return userService.registerUser(
                request.getName(),
                request.getEmail(),
                request.getPassword()
        );
    }

    // ================= LOGIN =================
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {

        return userService.loginUser(
                request.getEmail(),
                request.getPassword()
        );
    }

    // ================= PROFILE =================
    @GetMapping("/profile")
    public UserProfileDTO getProfile(Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserProfileDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    // ================= REFRESH TOKEN =================
    @PostMapping("/refresh")
    public AuthResponse refreshToken(@RequestBody Map<String, String> request) {

        String token = request.get("refreshToken");

        RefreshToken refreshToken =
                refreshTokenRepository.findByToken(token)
                        .orElseThrow(() ->
                                new CustomException("Invalid refresh token"));

        if (refreshToken.getExpiryDate()
                .isBefore(LocalDateTime.now())) {
            throw new CustomException("Refresh token expired");
        }

        String newAccessToken =
                jwtUtil.generateToken(
                        refreshToken.getUser().getEmail(),
                        refreshToken.getUser().getRole()
                );

        return new AuthResponse(newAccessToken, token);
    }

    // ================= MAKE ADMIN =================
    @GetMapping("/make-admin")
    public String makeAdmin(@RequestParam String email) {
        return userService.createAdmin(email);
    }
    @PostMapping("/forgot-password")
public String forgotPassword(@RequestBody Map<String,String> request) {

    String email = request.get("email");

    User user = userRepository.findByEmail(email)
            .orElseThrow(() ->
                    new CustomException("User not found"));

    String token = UUID.randomUUID().toString();

    user.setResetToken(token);
    user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));

    userRepository.save(user);

    return "Reset token: " + token; 
}
    @PostMapping("/reset-password")
public String resetPassword(@RequestBody Map<String,String> request) {

    String token = request.get("token");
    String newPassword = request.get("newPassword");

    User user = userRepository.findByResetToken(token)
            .orElseThrow(() ->
                    new CustomException("Invalid reset token"));

    if (user.getResetTokenExpiry()
            .isBefore(LocalDateTime.now())) {
        throw new CustomException("Reset token expired");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    user.setResetToken(null);
    user.setResetTokenExpiry(null);

    userRepository.save(user);

    return "Password reset successful";
}
    @GetMapping("/admin")
    public String adminOnly() {
        return "Admin Access Granted";
    }
}