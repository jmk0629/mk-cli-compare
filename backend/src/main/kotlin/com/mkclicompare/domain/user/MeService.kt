package com.mkclicompare.domain.user

import com.mkclicompare.web.error.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** 내 정보 조회/수정/탈퇴. 탈퇴는 soft delete(delete_yn=Y) → 이후 접근 차단. */
@Service
class MeService(
    private val userRepository: UserRepository,
) {
    @Transactional(readOnly = true)
    fun getActiveUser(userId: Long): User {
        val user = userRepository.findById(userId).orElseThrow {
            UnauthorizedException("존재하지 않는 사용자입니다.")
        }
        if (user.isWithdrawn()) throw UnauthorizedException("탈퇴한 계정입니다.")
        return user
    }

    @Transactional
    fun updateNickname(userId: Long, nickname: String): User {
        val user = getActiveUser(userId)
        user.nickname = nickname
        user.updatedAt = Instant.now().toString()
        return userRepository.save(user)
    }

    @Transactional
    fun withdraw(userId: Long) {
        val user = getActiveUser(userId)
        val now = Instant.now().toString()
        user.deleteYn = "Y"
        user.deletedAt = now
        user.updatedAt = now
        userRepository.save(user)
    }
}
