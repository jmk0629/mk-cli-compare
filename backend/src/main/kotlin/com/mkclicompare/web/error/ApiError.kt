package com.mkclicompare.web.error

import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

/** 표준 에러 응답 바디. */
data class ApiError(
    val code: String,
    val message: String,
    val details: String? = null,
)

/** 인증 실패(401) — 유효한 JWT 이지만 사용자 없음/탈퇴. */
class UnauthorizedException(message: String) : RuntimeException(message)

@RestControllerAdvice
class ApiExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val details = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(ApiError("VALIDATION", "Invalid request", details))
    }

    @ExceptionHandler(
        IllegalArgumentException::class,
        MethodArgumentTypeMismatchException::class,
        MissingServletRequestParameterException::class,
        ConstraintViolationException::class,
        HttpMessageNotReadableException::class,
    )
    fun handleBadRequest(ex: Exception): ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(ApiError("BAD_REQUEST", ex.message ?: "Bad request"))

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNotFound(ex: Exception): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError("NOT_FOUND", "요청한 경로를 찾을 수 없습니다."))

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError("UNAUTHORIZED", ex.message ?: "인증이 필요합니다."))

    @ExceptionHandler(Exception::class)
    fun handleAny(ex: Exception): ResponseEntity<ApiError> {
        log.error("Unhandled exception", ex)
        return ResponseEntity.internalServerError().body(ApiError("INTERNAL", ex.message ?: "Internal error"))
    }
}
