package com.nijat.ptlt.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

  private final SecretKey key;
  private final long accessTtlSeconds;
  private final long refreshTtlSeconds;

  public JwtService(@Value("${security.jwt.secret:ptlt-dev-secret-please-change-ptlt-dev-secret-please-change}") String secret,
                    @Value("${security.jwt.access-ttl-seconds:3600}") long accessTtlSeconds,
                    @Value("${security.jwt.refresh-ttl-seconds:2592000}") long refreshTtlSeconds) {
    // Ensure >= 256-bit
    byte[] bytes = secret.getBytes();
    if (bytes.length < 32) {
      secret = (secret + "_".repeat(32)).substring(0, 32);
    }
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
    this.accessTtlSeconds = accessTtlSeconds;
    this.refreshTtlSeconds = refreshTtlSeconds;
  }

  public String generateAccessToken(UUID userId, String email) {
    return generateToken(userId, email, accessTtlSeconds, Map.of("type", "access"));
  }

  public String generateRefreshToken(UUID userId, String email) {
    return generateToken(userId, email, refreshTtlSeconds, Map.of("type", "refresh"));
  }

  private String generateToken(UUID userId, String email, long ttlSeconds, Map<String, Object> extra) {
    Instant now = Instant.now();
    return Jwts.builder()
        .setSubject(userId.toString())
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(now.plusSeconds(ttlSeconds)))
        .addClaims(extra)
        .addClaims(Map.of("email", email))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }
}
