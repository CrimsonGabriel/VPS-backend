package com.bazunia.vps.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationFilter jwtAuthFilter; // ⭐️ WSTRZYKNIĘCIE FILTRA JWT ⭐️

    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())

                .authorizeHttpRequests(authz -> authz
                        // Endpointy publiczne (RPi, Logowanie, Status)
                        .requestMatchers("/register/rasp", "/register/android", "/update", "/data").permitAll()
                        .requestMatchers("/api/auth/**").permitAll() // Logowanie/Rejestracja
                        .requestMatchers("/auth/**", "/2fa/**").permitAll() // Google Auth/2FA
                        .requestMatchers("/status/json", "/").permitAll() // Status
                        .requestMatchers("/update").permitAll()
                        // --- ⭐️ CHRONIONE ENDPOINTY ⭐️ ---

                        // 1. Panel Admina: Wymaga tokena I roli ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 2. Pliki: Specjalna ochrona (MUSI BYĆ PRZED OGÓLNĄ REGULACJĄ DLA /api/files/**)
                        // ⭐️ DELETE wymaga roli ADMIN
                        .requestMatchers(HttpMethod.DELETE, "/api/files/**").hasRole("ADMIN")

                        // ⭐️ GET/POST/Inne na /api/files wymaga tylko uwierzytelnienia (USER lub ADMIN)
                        .requestMatchers("/api/files/**").authenticated()

                        // 3. Wszystko inne: Wymaga tokena
                        .anyRequest().authenticated()
                )

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint) // Obsługuje 401
                        .accessDeniedHandler(accessDeniedHandler)           // Obsługuje 403
                )

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}