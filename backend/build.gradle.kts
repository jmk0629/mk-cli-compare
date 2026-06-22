plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.14"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "1.9.25"
}

group = "com.mkclicompare"
version = "0.0.1-SNAPSHOT"
description = "Compare subscription coding CLIs (claude / agy / codex) on the same prompt"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	// 회원/SNS 로그인: OAuth2 client + Security + JWT.
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.flywaydb:flyway-core")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	// SQLite (dev/prod-volume). Postgres 전환 시 application-prod.yml + flyway-database-postgresql 추가.
	runtimeOnly("org.xerial:sqlite-jdbc:3.46.1.3")
	implementation("org.hibernate.orm:hibernate-community-dialects")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("io.mockk:mockk:1.13.13")
	testImplementation("com.ninja-squad:springmockk:4.0.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

/**
 * bootRun 시 프로젝트 루트의 `.env` 를 자동 로드 → 환경변수로 주입.
 *
 * 우선순위 (12-factor app): **shell env > .env 파일**.
 *   → `CLI_PARALLEL=false ./gradlew bootRun` 같이 임시 override 가능.
 *
 * 형식: `KEY=value` 라인만 인식. `#` 주석/빈 라인 무시. 따옴표 제거. 빈 값 (`KEY=`) 도 skip.
 */
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
	val envFile = rootProject.file("../.env")
	if (envFile.exists()) {
		envFile.readLines()
			.map { it.trim() }
			.filter { it.isNotBlank() && !it.startsWith("#") }
			.forEach { line ->
				val idx = line.indexOf('=')
				if (idx > 0) {
					val key = line.substring(0, idx).trim()
					val value = line.substring(idx + 1).trim().removeSurrounding("\"").removeSurrounding("'")
					if (value.isNotEmpty() && System.getenv(key) == null) {
						environment(key, value)
					}
				}
			}
	}
}
