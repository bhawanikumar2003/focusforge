package com.focusforge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }
    @Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
    @Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint entryPoint = (request, response, authException) ->
            response.sendError(401, "Unauthorized");

    http
    .csrf(csrf -> csrf.disable())
    .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    )
    .exceptionHandling(ex ->
            ex.authenticationEntryPoint(entryPoint)
    )
    .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/users/register").permitAll()
            .requestMatchers("/api/users/login").permitAll()
            .requestMatchers("/api/users/profile").hasRole("USER")
            .requestMatchers("/api/users/admin").hasAuthority("ROLE_ADMIN")
            .anyRequest().authenticated()
    )
    .addFilterBefore(jwtFilter,
            UsernamePasswordAuthenticationFilter.class);

    return http.build();
    
}
}