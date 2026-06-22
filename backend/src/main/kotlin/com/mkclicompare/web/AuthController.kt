package com.mkclicompare.web

import com.mkclicompare.config.OAuth2Properties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 인증 메타(공개). 프론트 로그인 페이지가 활성 provider 만 노출하도록. */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val oauth: OAuth2Properties,
) {
    @GetMapping("/providers")
    fun providers(): ProvidersRes = ProvidersRes(
        providers = buildList {
            if (oauth.google.enabled) add("google")
            if (oauth.kakao.enabled) add("kakao")
            if (oauth.naver.enabled) add("naver")
        },
    )
}

data class ProvidersRes(val providers: List<String>)
