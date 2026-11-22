package org.chengyuan.cyproject.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /** token 有效期（秒），用于让 Redis 缓存 TTL 与 JWT 保持一致 */
    public long getExpirationSeconds() {
        return expiration / 1000;
    }

    public String generateToken(String userid) {
        return Jwts.builder()
                .subject(userid)
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析 token 中的用户 id；解析失败返回 null，由调用方决定如何处理。
     */
    public String parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

}
