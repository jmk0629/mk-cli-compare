package com.mkclicompare.domain.comparison

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 비교 진행을 SSE 로 실시간 푸시. comparisonId 별 emitter 목록을 메모리에 보관하고,
 * ComparisonExecutor 가 run 갱신/완료 시 스냅샷을 발행한다. (단일 인스턴스 가정 — 스케일아웃 시 Redis pub/sub 등으로 확장)
 */
@Service
class ComparisonStreamService {
    private val log = LoggerFactory.getLogger(javaClass)
    private val emitters = ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>()

    fun subscribe(comparisonId: Long): SseEmitter {
        val emitter = SseEmitter(STREAM_TIMEOUT_MS)
        val list = emitters.computeIfAbsent(comparisonId) { CopyOnWriteArrayList() }
        list.add(emitter)
        emitter.onCompletion { list.remove(emitter) }
        emitter.onTimeout { list.remove(emitter); emitter.complete() }
        emitter.onError { list.remove(emitter) }
        return emitter
    }

    /** 스냅샷(현재 comparison+runs) 이벤트 발행. */
    fun publish(comparisonId: Long, eventName: String, payload: Any) {
        val list = emitters[comparisonId] ?: return
        list.forEach { emitter ->
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload))
            } catch (e: Exception) {
                log.debug("SSE send 실패(구독 종료 추정): {}", e.message)
                list.remove(emitter)
            }
        }
    }

    /** 완료 — done 이벤트 후 모든 emitter 종료. */
    fun complete(comparisonId: Long, payload: Any) {
        val list = emitters.remove(comparisonId) ?: return
        list.forEach { emitter ->
            try {
                emitter.send(SseEmitter.event().name("done").data(payload))
                emitter.complete()
            } catch (e: Exception) {
                log.debug("SSE complete 실패: {}", e.message)
            }
        }
    }

    private companion object {
        const val STREAM_TIMEOUT_MS = 5 * 60 * 1000L // 5분
    }
}
