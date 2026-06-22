package com.mkclicompare.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** SNS OAuth provider 설정 (`auth.oauth.*`). client-id 비면 해당 provider 자동 비활성(키 없이도 부팅). */
@ConfigurationProperties(prefix = "auth.oauth")
data class OAuth2Properties(
    val google: Provider = Provider(),
    val kakao: Provider = Provider(),
    val naver: Provider = Provider(),
) {
    data class Provider(
        val clientId: String = "",
        val clientSecret: String = "",
    ) {
        val enabled: Boolean get() = clientId.isNotBlank()
    }
}
