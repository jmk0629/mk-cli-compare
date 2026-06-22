package com.mkclicompare.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod

/**
 * SNS OAuth provider 등록을 프로그래밍 방식으로 구성. 키 없는 provider 는 제외(graceful).
 * 하나도 없으면 빈 자체를 안 만들어 SecurityConfig 가 oauth2Login 을 끈다.
 * 콘솔 redirect URI = `{baseUrl}/login/oauth2/code/{registrationId}`.
 */
@Configuration
class OAuth2ClientConfig(
    private val props: OAuth2Properties,
) {
    @Bean
    @Conditional(OAuthConfiguredCondition::class)
    fun clientRegistrationRepository(): ClientRegistrationRepository {
        val registrations = buildList {
            if (props.google.enabled) add(google(props.google))
            if (props.kakao.enabled) add(kakao(props.kakao))
            if (props.naver.enabled) add(naver(props.naver))
        }
        return InMemoryClientRegistrationRepository(registrations)
    }

    private fun google(p: OAuth2Properties.Provider): ClientRegistration =
        CommonOAuth2Provider.GOOGLE.getBuilder("google")
            .clientId(p.clientId)
            .clientSecret(p.clientSecret)
            .scope("profile", "email")
            .build()

    private fun kakao(p: OAuth2Properties.Provider): ClientRegistration =
        ClientRegistration.withRegistrationId("kakao")
            .clientId(p.clientId)
            .clientSecret(p.clientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(REDIRECT_URI_TEMPLATE)
            .scope("profile")
            .authorizationUri("https://kauth.kakao.com/oauth/authorize")
            .tokenUri("https://kauth.kakao.com/oauth/token")
            .userInfoUri("https://kapi.kakao.com/v2/user/me")
            .userNameAttributeName("id")
            .clientName("Kakao")
            .build()

    private fun naver(p: OAuth2Properties.Provider): ClientRegistration =
        ClientRegistration.withRegistrationId("naver")
            .clientId(p.clientId)
            .clientSecret(p.clientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(REDIRECT_URI_TEMPLATE)
            .scope("name", "email", "profile_image")
            .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
            .tokenUri("https://nid.naver.com/oauth2.0/token")
            .userInfoUri("https://openapi.naver.com/v1/nid/me")
            .userNameAttributeName("response")
            .clientName("Naver")
            .build()

    private companion object {
        const val REDIRECT_URI_TEMPLATE = "{baseUrl}/login/oauth2/code/{registrationId}"
    }
}

/** auth.oauth.{google,kakao,naver}.client-id 중 하나라도 설정됐는지. */
class OAuthConfiguredCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        listOf("google", "kakao", "naver").any { provider ->
            !context.environment.getProperty("auth.oauth.$provider.client-id").isNullOrBlank()
        }
}
