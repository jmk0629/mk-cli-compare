package com.mkclicompare.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.mkclicompare.auth.JwtAuthFilter
import com.mkclicompare.auth.JwtTokenProvider
import com.mkclicompare.auth.oauth.CustomOAuth2UserService
import com.mkclicompare.auth.oauth.OAuth2FailureHandler
import com.mkclicompare.auth.oauth.OAuth2SuccessHandler
import com.mkclicompare.web.error.ApiError
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Spring Security 필터 체인 — 게스트 우선.
 * - `api/me` 하위만 JWT 인증 필요. 그 외(게임 카탈로그/세션 제출 등) permitAll → 비로그인 동작.
 * - 무상태(JWT in Authorization). OAuth 핸드셰이크 동안만 transient 세션.
 * - provider 미설정 시 oauth2Login 비활성(키 없이도 부팅).
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider,
    private val objectMapper: ObjectMapper,
    @Value("\${cors.allowed-origins}") private val allowedOrigins: String,
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2SuccessHandler: OAuth2SuccessHandler,
    private val oAuth2FailureHandler: OAuth2FailureHandler,
    private val clientRegistrationRepository: ObjectProvider<ClientRegistrationRepository>,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/api/me/**").authenticated()
                it.anyRequest().permitAll()
            }
            .exceptionHandling { eh ->
                eh.authenticationEntryPoint { _, response, _ ->
                    response.status = 401
                    response.contentType = "application/json;charset=UTF-8"
                    response.writer.write(
                        objectMapper.writeValueAsString(
                            ApiError(code = "UNAUTHORIZED", message = "로그인이 필요합니다."),
                        ),
                    )
                }
            }
            .addFilterBefore(
                JwtAuthFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter::class.java,
            )

        if (clientRegistrationRepository.ifAvailable != null) {
            http.oauth2Login { oauth ->
                oauth.userInfoEndpoint { it.userService(customOAuth2UserService) }
                oauth.successHandler(oAuth2SuccessHandler)
                oauth.failureHandler(oAuth2FailureHandler)
            }
        }
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = this@SecurityConfig.allowedOrigins.split(",").map { it.trim() }
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = false
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", config)
        }
    }
}
