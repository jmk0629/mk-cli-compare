---
name: commit-push
description: mk-cli-compare 변경을 의미 단위로 stage → conventional commit → git push origin main. 시크릿(.env)·force push 를 차단하고 안전하게 자동 커밋/푸시한다.
---

# commit-push

mk-cli-compare 의 변경사항을 안전하게 커밋하고 `https://github.com/jmk0629/mk-cli-compare` 로 푸시한다.

## 입력 (선택)

- `message` — 커밋 메시지(생략 시 변경 내용으로 자동 작성)
- `scope` — 커밋 범위 힌트 (예: `backend`, `frontend`, `provider`, `auth`, `docs`)

## 동작

1. **현황 파악**: `git status`, `git diff`, `git log --oneline -5` 를 병렬로 실행.
2. **시크릿 가드** (차단 = 커밋 중단):
   - `.env`, `*.env.local`, `*credential*`, `*secret*`, `*.db`, `data/` 가 staging 대상이면 **제외**하고 경고.
   - diff 에 OAuth client secret / JWT secret / API 키 패턴(`GOCSPX-`, `AIza`, 하드코딩 `Bearer `)이 보이면 중단·보고.
3. **stage**: 관련 파일만 명시적으로 `git add <files>`. `git add -A`/`.` 지양(시크릿 혼입 방지).
4. **commit**: Conventional Commits. `feat:` `fix:` `refactor:` `docs:` `test:` `chore:` `style:`
   - **제목(첫 줄)은 한국어로** 사용자가 한눈에 이해할 수 있게 쓴다. `feat:`/`fix:` 등 타입 프리픽스와
     코드 식별자(클래스/파일/플래그명)는 그대로 영어로 둔다.
     예: `feat: CLI 비교 엔진 추가 (claude/agy/codex)`, `fix: codex 출력 파싱 오류 수정`.
   - 단, **영어로만 써도 자연스러운 경우(짧은 기술 용어 위주)는 영어 허용** — 판단해서 더 명확한 쪽으로.
   - 제목 72자 이내 + 본문(왜, 한국어). 끝에 `Co-Authored-By: Claude` 추가.
5. **push**: `git push origin main` (최초엔 `git push -u origin main`).
   - **force push 절대 금지.** 거부되면 `git pull --rebase` 후 재시도, 충돌은 사용자 보고.

## 금지

- `git push --force` / `-f`
- `.env`/`*.db` 등 시크릿·로컬 데이터 커밋
- 사용자 미확인 대규모 삭제

## 출력

커밋 해시 + 푸시 결과 + (있다면) 제외한 시크릿 파일 목록.
