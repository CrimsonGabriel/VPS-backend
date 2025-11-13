package com.bazunia.vps.service;

// IMPORTY DLA JWT
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

// IMPORT DLA SPRING SECURITY
import org.springframework.security.core.userdetails.UserDetails;

// IMPORTY JAVY
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// TEN IMPORT JEST KLUCZOWY
import org.springframework.beans.factory.annotation.Value;

@Service
public class JwtService {

    private final String secretKey;

    // TWORZYMY KONSTRUKTOR, KTÓRY WSTRZYKUJE KLUCZ
    public JwtService(@Value("${jwt.secret.key}") String secretKey) {
        this.secretKey = secretKey;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * ⭐️ POPRAWIONA METODA: Generuje token JWT dodając rolę użytkownika.
     */
    public String generateToken(UserDetails userDetails) {
        // 1. POBIERAMY ROLĘ
        String role = userDetails.getAuthorities().stream()
                .findFirst() // Zakładamy, że jest tylko jedna rola na użytkownika
                // Używamy replace, aby usunąć prefiks "ROLE_" i zostawić tylko "ADMIN" lub "USER"
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElse("USER");

        // 2. TWORZYMY MAPĘ Z ROLĄ
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role); // DODAJEMY KLUCZ "role" DO CLAIMS

        // 3. GENERUJEMY TOKEN Z TYMI ROLAMI
        return generateToken(claims, userDetails);
    }

    // Metoda wewnętrzna do faktycznego budowania tokena
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts
                .builder()
                .setClaims(extraClaims) // Ustawiamy Claims (w tym naszą nowo dodaną rolę)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSignInKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}