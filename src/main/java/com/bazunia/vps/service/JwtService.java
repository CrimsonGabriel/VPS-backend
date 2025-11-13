// 💾 JwtService.java (DODANA METODA DEBUGUJĄCA)

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

    public String generateToken(UserDetails userDetails) {
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElse("USER");

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);

        return generateToken(claims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
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

    // ⭐️⭐️⭐️ NOWA METODA DEBUGUJĄCA ⭐️⭐️⭐️
    /**
     * Zwraca klucz tajny, którego aktualnie używa ten serwis.
     * Używane tylko przez nasz nowy endpoint /debug-key
     */
    public String getSecretKeyForDebug() {
        return this.secretKey;
    }
}