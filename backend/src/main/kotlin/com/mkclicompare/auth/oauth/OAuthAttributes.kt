package com.mkclicompare.auth.oauth

/** provider 별 사용자정보 응답 → 공통 필드 추출. */
data class OAuthAttributes(
    val providerId: String,
    val email: String?,
    val nickname: String?,
    val profileImage: String?,
) {
    companion object {
        fun extract(registrationId: String, attributes: Map<String, Any?>): OAuthAttributes =
            when (registrationId) {
                "google" -> google(attributes)
                "kakao" -> kakao(attributes)
                "naver" -> naver(attributes)
                else -> throw IllegalArgumentException("지원하지 않는 provider: $registrationId")
            }

        private fun google(a: Map<String, Any?>) = OAuthAttributes(
            providerId = a["sub"].toString(),
            email = a["email"] as? String,
            nickname = a["name"] as? String,
            profileImage = a["picture"] as? String,
        )

        @Suppress("UNCHECKED_CAST")
        private fun kakao(a: Map<String, Any?>): OAuthAttributes {
            val account = a["kakao_account"] as? Map<String, Any?>
            val profile = account?.get("profile") as? Map<String, Any?>
            return OAuthAttributes(
                providerId = a["id"].toString(),
                email = account?.get("email") as? String,
                nickname = profile?.get("nickname") as? String,
                profileImage = profile?.get("profile_image_url") as? String,
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun naver(a: Map<String, Any?>): OAuthAttributes {
            val response = a["response"] as? Map<String, Any?>
                ?: throw IllegalArgumentException("naver 응답에 response 누락")
            return OAuthAttributes(
                providerId = response["id"].toString(),
                email = response["email"] as? String,
                nickname = (response["nickname"] as? String) ?: (response["name"] as? String),
                profileImage = response["profile_image"] as? String,
            )
        }
    }
}
