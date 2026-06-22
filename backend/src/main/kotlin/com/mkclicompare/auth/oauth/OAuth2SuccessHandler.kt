package com.mkclicompare.auth.oauth

import com.mkclicompare.auth.JwtTokenProvider
import com.mkclicompare.config.AuthProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

/** OAuth 성공 → JWT 발급 → `{webUrl}/auth/callback#token=<JWT>` 로 redirect(fragment). */
@Component
class OAuth2SuccessHandler(
    private val jwtTokenProvider: JwtTokenProvider,
    private val authProperties: AuthProperties,
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val principal = authentication.principal as OAuth2User
        val userId = (principal.attributes[CustomOAuth2UserService.ATTR_USER_ID] as? String)?.toLongOrNull()
        if (userId == null) {
            response.sendRedirect("${authProperties.webUrl}/auth/callback#error=login_failed")
            return
        }
        val provider = principal.attributes[CustomOAuth2UserService.ATTR_PROVIDER] as? String ?: ""
        val nickname = (principal.attributes[CustomOAuth2UserService.ATTR_NICKNAME] as? String)?.ifBlank { null }
        val token = jwtTokenProvider.issue(userId, provider, nickname)
        response.sendRedirect("${authProperties.webUrl}/auth/callback#token=$token")
    }
}
