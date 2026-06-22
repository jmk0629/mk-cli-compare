package com.mkclicompare.auth

/** JWT 로 인증된 사용자 principal. SecurityContext 의 Authentication.principal 로 들어간다. */
data class AuthenticatedUser(val userId: Long)
