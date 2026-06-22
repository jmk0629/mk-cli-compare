package com.mkclicompare.auth

import com.mkclicompare.config.AuthProperties
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

/** JWT 발급/검증 (HS256). subject = userId. 검증 실패는 전부 null 로 수렴. */
@Component
class JwtTokenProvider(authProperties: AuthProperties) {

    private val key: SecretKey = Keys.hmacShaKeyFor(authProperties.jwt.secret.toByteArray())
    private val expirySeconds: Long = authProperties.jwt.expiryMinutes * 60

    fun issue(userId: Long, provider: String, nickname: String?): String =
        issueAt(userId, provider, nickname, Instant.now())

    fun issueAt(userId: Long, provider: String, nickname: String?, issuedAt: Instant): String =
        Jwts.builder()
            .subject(userId.toString())
            .claim("provider", provider)
            .claim("nickname", nickname)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(issuedAt.plusSeconds(expirySeconds)))
            .signWith(key)
            .compact()

    fun validateAndGetUserId(token: String): Long? =
        try {
            Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).payload.subject.toLongOrNull()
        } catch (e: JwtException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
}
