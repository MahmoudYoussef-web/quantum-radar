package com.quradar.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessMillis;
    private final long refreshMillis;

    public JwtService(@Value("${quradar.jwt.secret}") String secret,
                      @Value("${quradar.jwt.access-minutes:15}") long accessMinutes,
                      @Value("${quradar.jwt.refresh-days:7}") long refreshDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessMillis = accessMinutes * 60_000L;
        this.refreshMillis = refreshDays * 86_400_000L;
    }

    public String accessToken(UserEntity user) {
        return build(user.getUsername(), "access", null, accessMillis, user);
    }

    public String refreshToken(UserEntity user) {
        return build(user.getUsername(), "refresh", UUID.randomUUID().toString(), refreshMillis, user);
    }

    public long refreshMillis() {
        return refreshMillis;
    }

    private String build(String username, String type, String jti, long ttlMillis, UserEntity user) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(username)
                .claim("type", type)
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMillis)))
                .signWith(key);
        if (jti != null) {
            builder.id(jti);
        }
        if (user.getDriver() != null) {
            builder.claim("driverLicenseNo", user.getDriver().getLicenseNo());
        }
        if (user.getDevice() != null) {
            builder.claim("deviceCode", user.getDevice().getDeviceCode());
        }
        return builder.compact();
    }

    /** Throws JwtException on invalid signature / expiry / malformed token. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
