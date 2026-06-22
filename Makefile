.PHONY: help backend frontend install test clean doctor

help:
	@echo "mk-cli-compare — 사용 가능한 명령"
	@echo "  make doctor    — 설치된 CLI(claude/agy/codex) 점검"
	@echo "  make install   — 프론트 의존성 설치 (pnpm)"
	@echo "  make backend   — Spring Boot 백엔드 실행 (8080)"
	@echo "  make frontend  — Next.js 프론트 실행 (3000)"
	@echo "  make test      — 백/프론 테스트 실행"
	@echo "  make clean     — 빌드 산출물 정리"
	@echo ""
	@echo "  ⚠️  mk-hospital / mk-health-app / mk-remember-game 과 8080/3000 공유 → 동시 실행 금지"

doctor:
	@echo "── CLI 점검 (비교 엔진) ──"
	@for c in claude agy codex; do \
	  if command -v $$c >/dev/null 2>&1; then echo "  ✓ $$c → $$(command -v $$c)"; \
	  else echo "  ✗ $$c (미설치 — 해당 provider 비활성)"; fi; \
	done

install:
	cd frontend && pnpm install

backend:
	cd backend && ./gradlew bootRun

frontend:
	cd frontend && pnpm dev

test:
	cd backend && ./gradlew test
	cd frontend && pnpm tsc --noEmit

clean:
	cd backend && ./gradlew clean
	rm -rf frontend/.next frontend/out
