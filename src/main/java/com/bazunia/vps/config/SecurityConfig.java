package com.bazunia.vps.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationFilter jwtAuthFilter;

    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Konfiguracja CORS bez zmian

                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // --- 1. ENDPOINTY PUBLICZNE ---
                        // (Logowanie, Rejestracja, Reset Hasła, RPi, Status)
                        .requestMatchers(
                                "/",
                                "/status/json",
                                "/debug-key",
                                // RPi (chronione hasłem w kontrolerze)
                                "/register/rasp",
                                "/register/android",
                                "/update",
                                "/data",
                                "/api/sensors/battery",
                                "/api/sensors/config",
                                // Logowanie i Rejestracja
                                "/api/auth/google",
                                "/api/auth/login",
                                "/api/auth/android/register",
                                "/api/auth/android/activate",
                                // Weryfikacja 2FA (tylko przy logowaniu)
                                "/api/auth/2fa/login-verify",
                                "/api/auth/2fa/email-verify",
                                // Reset hasła
                                "/api/auth/request-password-reset",
                                "/api/auth/reset-password",
                                // frontend
                                "/updates",
                                "/updates/**"

                        ).permitAll()

                        // --- 2. ENDPOINTY CHRONIONE (Wymagają JWT) ---
                        .requestMatchers(
                                // <<< POPRAWKA: JAWNIE ZEZWÓL NA TE ŚCIEŻKI >>>
                                "/data/android",
                                "/api/gateways",
                                "/api/gateways/share",
                                "/api/gateways/**",
                                "/api/sensors/**",
                                "/api/status/**",
                                "/api/users/**",
                                "/api/favorites/**",
                                "/api/user/me",
                                "/api/user/set-password",
                                "/api/user/change-password",
                                "/api/auth/2fa/status",
                                "/api/auth/2fa/setup",
                                "/api/auth/2fa/verify",
                                "/api/auth/2fa/disable",
                                "/api/update/**",
                                "/api/files/**",
                                "/api/folders/**",
                                "/api/retention/**"
                        ).authenticated()

                        // --- 3. ENDPOINTY ADMINA (Wymagają Roli ADMIN) ---
                        .requestMatchers(
                                "/api/admin/**",
                                "/api/data/history/delete"
                        ).hasRole("ADMIN")

                        // Cała reszta (jeśli coś pominięto) też wymaga logowania
                        .anyRequest().authenticated()
                )

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}