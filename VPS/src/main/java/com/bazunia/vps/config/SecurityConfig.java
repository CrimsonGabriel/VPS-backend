package com.bazunia.vps.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// ⭐️ POPRAWKA 1: Dodanie importów dla konfiguracji CORS
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

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
                .csrf(csrf -> csrf.disable())
                // ⭐️ POPRAWKA 2: Poprawna konfiguracja CORS zamiast .cors(cors -> cors.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests(authz -> authz
                        // ⭐️ POPRAWKA 3: Jawne zezwolenie na żądania OPTIONS (Preflight)
                        // Ta reguła musi być JEDNĄ Z PIERWSZYCH.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers("/debug-key").permitAll()
                        // Endpointy publiczne (RPi, Logowanie, Status) - BEZ ZMIAN
                        .requestMatchers("/register/rasp", "/register/android", "/update", "/data").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/auth/**", "/2fa/**").permitAll()
                        .requestMatchers("/status/json", "/").permitAll()
                        .requestMatchers("/update").permitAll()

                        // --- CHRONIONE ENDPOINTY (BEZ ZMIAN) ---
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/files/**").hasRole("ADMIN")
                        .requestMatchers("/api/files/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/data/history/delete").hasRole("ADMIN")
                        .requestMatchers("/api/update/**").authenticated()
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

    // ⭐️ POPRAWKA 4: Dodanie Beana konfigurującego CORS
    // To mówi serwerowi, aby ufał żądaniom z Twojego frontendu
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Zezwalamy na żądania z dowolnego miejsca (dla prostoty)
        // W produkcji można tu wpisać np. "https://testserwera.pl"
        configuration.setAllowedOrigins(Arrays.asList("*"));
        // Zezwalamy na wszystkie kluczowe metody
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Zezwalamy na wszystkie nagłówki (kluczowe dla 'Authorization')
        configuration.setAllowedHeaders(Arrays.asList("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Stosujemy tę konfigurację do wszystkich ścieżek
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}