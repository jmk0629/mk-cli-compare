package com.mkclicompare.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** 회원/JWT 인증 설정 (application.yml `auth.*`). 크로스오리진 → JWT in Authorization 헤더. */
@ConfigurationProperties(prefix = "auth")
data class AuthProperties(
    val jwt: Jwt,
    val webUrl: String,
) {
    data class Jwt(
        val secret: String,
        val expiryMinutes: Long = 10080,
    )
}
