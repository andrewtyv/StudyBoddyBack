package security;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;



//GPT all
@Component
public class JwtUtil {

    private final Key signingKey;
    private final long expirationMillis;
    private final String issuer;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-millis:3600000}") long expirationMillis,
            @Value("${jwt.issuer:StudyBuddy}") String issuer,
            @Value("${jwt.secret-base64:true}") boolean useBase64Secret
    ) {
        this.expirationMillis = expirationMillis;
        this.issuer = issuer;

        byte[] keyBytes = useBase64Secret ? Decoders.BASE64.decode(secret) : secret.getBytes();
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username) {
        return generateToken(username, Map.of());
    }

    public String generateToken(String username, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(expirationMillis);

        JwtBuilder builder = Jwts.builder()
                .setIssuer(issuer)
                .setSubject(username)                 // sub
                .setIssuedAt(Date.from(now))          // iat
                .setExpiration(Date.from(exp))        // exp
                .signWith(signingKey, SignatureAlgorithm.HS256);

        if (extraClaims != null && !extraClaims.isEmpty()) {
            builder.addClaims(extraClaims);
        }

        return builder.compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Claims extractAllClaims(String token) {
        return parseToken(token).getBody();
    }

    public boolean isTokenValid(String token, String expectedUsername) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenUsername = claims.getSubject();
            return tokenUsername != null
                    && tokenUsername.equals(expectedUsername)
                    && !isTokenExpired(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !isTokenExpired(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTokenExpired(Claims claims) {
        Date exp = claims.getExpiration();
        return exp == null || exp.before(new Date());
    }

    private Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token);
    }
}