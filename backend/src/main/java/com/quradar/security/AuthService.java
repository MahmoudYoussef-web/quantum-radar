package com.quradar.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository tokens;
    private final PasswordEncoder passwords;
    private final JwtService jwt;

    public AuthService(UserRepository users, RefreshTokenRepository tokens,
                       PasswordEncoder passwords, JwtService jwt) {
        this.users = users;
        this.tokens = tokens;
        this.passwords = passwords;
        this.jwt = jwt;
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {
    }

    @Transactional
    public TokenPair login(String username, String password) {
        UserEntity user = users.findByUsername(username)
                .filter(UserEntity::isEnabled)
                .filter(u -> passwords.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        return issue(user);
    }

    /** Rotation: the presented refresh token is revoked and a new pair is issued. */
    @Transactional
    public TokenPair refresh(String refreshToken) {
        Claims claims = parseRefresh(refreshToken);
        RefreshToken stored = tokens.findByTokenHash(sha256(refreshToken))
                .filter(t -> !t.isRevoked())
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new BadCredentialsException("Refresh token revoked or unknown"));
        if (!stored.getUser().getUsername().equals(claims.getSubject())) {
            throw new BadCredentialsException("Refresh token mismatch");
        }
        stored.setRevoked(true);
        return issue(stored.getUser());
    }

    @Transactional
    public void logout(String refreshToken) {
        tokens.findByTokenHash(sha256(refreshToken)).ifPresent(t -> t.setRevoked(true));
    }

    private TokenPair issue(UserEntity user) {
        String refresh = jwt.refreshToken(user);
        tokens.save(new RefreshToken(user, sha256(refresh),
                Instant.now().plusMillis(jwt.refreshMillis())));
        return new TokenPair(jwt.accessToken(user), refresh, 15 * 60L);
    }

    private Claims parseRefresh(String refreshToken) {
        try {
            Claims claims = jwt.parse(refreshToken);
            if (!"refresh".equals(claims.get("type", String.class))) {
                throw new BadCredentialsException("Not a refresh token");
            }
            return claims;
        } catch (JwtException ex) {
            throw new BadCredentialsException("Invalid refresh token");
        }
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
