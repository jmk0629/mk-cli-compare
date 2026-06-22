package com.mkclicompare.web

import com.mkclicompare.auth.AuthenticatedUser
import com.mkclicompare.domain.user.MeService
import com.mkclicompare.domain.user.User
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** 내 정보 — JWT 인증 필요. */
@RestController
@RequestMapping("/api/me")
class MeController(
    private val meService: MeService,
) {
    @GetMapping
    fun me(@AuthenticationPrincipal principal: AuthenticatedUser): MeRes =
        MeRes.from(meService.getActiveUser(principal.userId))

    @PatchMapping
    fun update(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @RequestBody @Valid request: UpdateMeReq,
    ): MeRes = MeRes.from(meService.updateNickname(principal.userId, request.nickname))

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdraw(@AuthenticationPrincipal principal: AuthenticatedUser) {
        meService.withdraw(principal.userId)
    }
}

data class UpdateMeReq(
    @field:Size(min = 1, max = 20, message = "닉네임은 1~20자입니다.")
    val nickname: String,
)

data class MeRes(
    val id: Long,
    val provider: String,
    val email: String?,
    val nickname: String?,
    val profileImage: String?,
    val createdAt: String,
) {
    companion object {
        fun from(u: User): MeRes = MeRes(
            id = requireNotNull(u.id),
            provider = u.provider,
            email = u.email,
            nickname = u.nickname,
            profileImage = u.profileImage,
            createdAt = u.createdAt,
        )
    }
}
