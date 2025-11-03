package com.bazunia.vps.config;

// IMPORTY DLA SPRING SECURITY
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // Ta adnotacja włącza Spring Security
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Wyłączamy CSRF
                .cors(cors -> cors.disable()) // Na razie wyłączamy, potem skonfigurujemy

                // Mówimy Springowi, że nie używamy sesji (bo używamy JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(authz -> authz
                        // Endpointy dla RPi (sprawdzimy hasło ręcznie w kontrolerze)
                        .requestMatchers("/register/rasp", "/register/android", "/update", "/data").permitAll()

                        // Endpointy do logowania (każdy może się zalogować)
                        .requestMatchers("/auth/google", "/auth/2fa/login-verify").permitAll()

                        // Endpointy statusowe (publiczne)
                        .requestMatchers("/status/json", "/").permitAll()

                        // Cała reszta (np. /data/android, /2fa/setup) musi być chroniona
                        .anyRequest().authenticated()
                );

        // TODO: Tutaj później dodamy filtr JWT
        // .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}