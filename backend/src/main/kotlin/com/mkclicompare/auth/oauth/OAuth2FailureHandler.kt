package com.mkclicompare.auth.oauth

import com.mkclicompare.config.AuthProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component

/** OAuth 실패 → `{webUrl}/auth/callback#error=<code>` 로 redirect. */
@Component
class OAuth2FailureHandler(
    private val authProperties: AuthProperties,
) : AuthenticationFailureHandler {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        val errorCode = (exception as? OAuth2AuthenticationException)?.error?.errorCode ?: "login_failed"
        response.sendRedirect("${authProperties.webUrl}/auth/callback#error=$errorCode")
    }
}
