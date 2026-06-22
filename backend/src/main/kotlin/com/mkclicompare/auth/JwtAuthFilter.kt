package com.mkclicompare.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * `Authorization: Bearer <jwt>` 를 읽어 유효하면 SecurityContext 에 인증을 채운다.
 * 토큰 없거나 무효면 통과(공개 API 무영향). @Component 아님(SecurityConfig 에서 직접 생성).
 */
class JwtAuthFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            val token = header.substring(BEARER_PREFIX.length)
            val userId = jwtTokenProvider.validateAndGetUserId(token)
            if (userId != null && SecurityContextHolder.getContext().authentication == null) {
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(AuthenticatedUser(userId), null, emptyList())
            }
        }
        filterChain.doFilter(request, response)
    }

    private companion object {
        const val BEARER_PREFIX = "Bearer "
    }
}
