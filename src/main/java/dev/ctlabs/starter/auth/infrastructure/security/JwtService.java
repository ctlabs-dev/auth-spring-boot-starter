package dev.ctlabs.starter.auth.infrastructure.security;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for JWT (JSON Web Token) operations.
 * Handles token generation, validation, and claim extraction.
 */
@Service
public class JwtService {

    private final AuthProperties authProperties;

    public JwtService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * @param token The JWT token.
     * @return The username.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a JWT token for a user with default claims.
     *
     * @param userDetails The user details.
     * @return The generated JWT token.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a JWT token for a user with extra claims.
     *
     * @param extraClaims Additional claims to include in the token.
     * @param userDetails The user details.
     * @return The generated JWT token.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(
                        System.currentTimeMillis() + authProperties.getJwt().getExpiration()))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Validates a JWT token against user details.
     *
     * @param token       The JWT token.
     * @param userDetails The user details.
     * @return True if the token is valid and belongs to the user, false otherwise.
     */
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
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(authProperties.getJwt().getSecretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
