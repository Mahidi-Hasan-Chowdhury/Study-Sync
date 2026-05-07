package bd.edu.seu.studysync.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    // Secret key must be at least 256 bits (32 characters) for HS256
    private final byte[] JWT_SECRET = "StudySyncSecretKey123456789012345678901234567890".getBytes(StandardCharsets.UTF_8);
    private final SecretKey JWT_SECRET_KEY = Keys.hmacShaKeyFor(JWT_SECRET);
    
    // Token expiration: 24 hours
    private final long EXPIRATION_TIME = 86400000; // 24 hours in milliseconds
    
    /**
     * Generate JWT token for a user
     */
    public String generateToken(String username, String role) {
        Map<String, String> payload = Map.of("role", role);
        return Jwts.builder()
                .subject(username)
                .claims(payload)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(JWT_SECRET_KEY)
                .compact();
    }
    
    /**
     * Parse JWT token and extract claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(JWT_SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * Extract username from token
     */
    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }
    
    /**
     * Extract role from token
     */
    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }
    
    /**
     * Validate token expiration
     */
    public boolean isTokenExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
