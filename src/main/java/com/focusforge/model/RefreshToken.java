package com.focusforge.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    private LocalDateTime expiryDate;

    @OneToOne
    private User user;

    public RefreshToken() {}

    public RefreshToken(String token,
                        LocalDateTime expiryDate,
                        User user) {
        this.token = token;
        this.expiryDate = expiryDate;
        this.user = user;
    }

    public String getToken() { return token; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public User getUser() { return user; }
}