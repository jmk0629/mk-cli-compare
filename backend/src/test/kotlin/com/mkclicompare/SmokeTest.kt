package com.mkclicompare

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

/**
 * 통합 스모크 — 전체 컨텍스트 부팅 + Flyway 마이그레이션/시드 + 보안 배선을 한 번에 검증.
 * 단위 테스트가 못 잡는 빈 배선/마이그레이션/시큐리티 회귀를 막는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SmokeTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `context loads and health is up`() {
        mockMvc.get("/actuator/health").andExpect { status { isOk() } }
    }

    @Test
    fun `providers are public and seeded with 3 CLIs`() {
        mockMvc.get("/api/providers").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(3) }
        }
    }

    @Test
    fun `presets are public and seeded`() {
        mockMvc.get("/api/presets").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(23) } // V1(7) + V5(16)
        }
    }

    @Test
    fun `leaderboard is public and lists all providers`() {
        mockMvc.get("/api/leaderboard").andExpect {
            status { isOk() }
            jsonPath("$.rankings.length()") { value(3) }
        }
    }

    @Test
    fun `me endpoint requires auth`() {
        mockMvc.get("/api/me").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `my comparisons requires auth`() {
        mockMvc.get("/api/me/comparisons").andExpect { status { isUnauthorized() } }
    }
}
