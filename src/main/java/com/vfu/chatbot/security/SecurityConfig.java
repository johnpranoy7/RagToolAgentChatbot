package com.vfu.chatbot.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())  // ✅ No CSRF 403s
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/static/**", "/api/**").permitAll()  // ✅ All your endpoints
                        .requestMatchers("/api/chat/**").permitAll()
                        .requestMatchers("/api/reset").permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}
