package com.bazunia.vps.config;

import com.bazunia.vps.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    // Potrzebujemy UserDetailsService, aby załadować użytkownika po emailu z tokena
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Sprawdź, czy żądanie zawiera nagłówek Bearer Token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Wyciągnij token
        jwt = authHeader.substring(7);

        // 3. Wyciągnij email z tokena
        try {
            userEmail = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            // Token jest nieprawidłowy/wygaśnięty
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Jeśli email istnieje i użytkownik nie jest jeszcze zalogowany w kontekście
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Wczytaj dane użytkownika (w tym role) z bazy
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 5. Sprawdź, czy token jest ważny
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // Utwórz obiekt uwierzytelnienia Spring Security
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities() // Wczytane role/uprawnienia (w tym Rola ADMIN)
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Ustaw obiekt uwierzytelnienia w kontekście bezpieczeństwa
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 6. Przekaż żądanie dalej w łańcuchu filtrów
        filterChain.doFilter(request, response);
    }
}