package com.mkclicompare.auth.oauth

import com.mkclicompare.domain.user.User
import com.mkclicompare.domain.user.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** OAuth 사용자정보 → 회원 upsert. (provider, providerId) 조회 → 없으면 신규, 있으면 최신화. 탈퇴 차단. */
@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
) : OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    internal var delegate: OAuth2UserService<OAuth2UserRequest, OAuth2User> = DefaultOAuth2UserService()

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oauthUser = delegate.loadUser(userRequest)
        val registrationId = userRequest.clientRegistration.registrationId
        val info = OAuthAttributes.extract(registrationId, oauthUser.attributes)

        val existing = userRepository.findByProviderAndProviderId(registrationId, info.providerId)
        if (existing != null && existing.isWithdrawn()) {
            throw OAuth2AuthenticationException(OAuth2Error(ERROR_WITHDRAWN), "탈퇴한 계정입니다.")
        }

        val now = Instant.now().toString()
        val user = if (existing == null) {
            userRepository.save(
                User(
                    provider = registrationId,
                    providerId = info.providerId,
                    email = info.email,
                    nickname = info.nickname,
                    profileImage = info.profileImage,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            info.email?.let { existing.email = it }
            info.nickname?.let { existing.nickname = it }
            info.profileImage?.let { existing.profileImage = it }
            existing.updatedAt = now
            userRepository.save(existing)
        }

        val attributes = oauthUser.attributes.toMutableMap()
        attributes[ATTR_USER_ID] = user.id.toString()
        attributes[ATTR_PROVIDER] = registrationId
        attributes[ATTR_NICKNAME] = user.nickname ?: ""
        return DefaultOAuth2User(
            listOf(SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            ATTR_USER_ID,
        )
    }

    companion object {
        const val ATTR_USER_ID = "appUserId"
        const val ATTR_PROVIDER = "appProvider"
        const val ATTR_NICKNAME = "appNickname"
        const val ERROR_WITHDRAWN = "account_withdrawn"
    }
}
