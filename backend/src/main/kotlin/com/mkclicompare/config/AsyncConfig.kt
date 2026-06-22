package com.mkclicompare.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/** 비교 비동기 실행 풀. HTTP 스레드를 점유하지 않고 CLI 실행을 백그라운드로 돌린다. */
@Configuration
class AsyncConfig {
    @Bean(name = ["comparisonTaskExecutor"])
    fun comparisonTaskExecutor(): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 4
            maxPoolSize = 12
            queueCapacity = 100
            setThreadNamePrefix("cmp-exec-")
            initialize()
        }
}
