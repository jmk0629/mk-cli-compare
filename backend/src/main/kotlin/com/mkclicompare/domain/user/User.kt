package com.mkclicompare.domain.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/** 회원(SNS 소셜 로그인). (provider, providerId) 가 계정 1행을 식별. 탈퇴는 soft delete. */
@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "provider", nullable = false)
    val provider: String = "",

    @Column(name = "provider_id", nullable = false)
    val providerId: String = "",

    @Column(name = "email")
    var email: String? = null,

    @Column(name = "nickname")
    var nickname: String? = null,

    @Column(name = "profile_image")
    var profileImage: String? = null,

    @Column(name = "delete_yn", nullable = false)
    var deleteYn: String = "N",

    @Column(name = "deleted_at")
    var deletedAt: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: String = "",

    @Column(name = "updated_at", nullable = false)
    var updatedAt: String = "",
) {
    fun isWithdrawn(): Boolean = deleteYn == "Y"
}
