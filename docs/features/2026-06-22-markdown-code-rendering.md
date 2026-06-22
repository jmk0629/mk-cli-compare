# 2026-06-22 — 응답 마크다운/코드 렌더링 + 복사 버튼

## 요청
- CLI 응답은 마크다운/코드 블록 → 평문 그대로 보면 가독성이 나쁨. 멋지게 렌더 + 복사 버튼.

## 만든 것
- `Markdown` 컴포넌트: `react-markdown` + `remark-gfm`(표·체크박스·취소선) + `rehype-highlight`(코드 하이라이트, highlight.js github 테마).
- `CopyButton` 컴포넌트: 응답 전체를 클립보드 복사, "✓ 복사됨" 피드백.
- 적용: 결과 카드(`ResultCard`)와 비교 상세(`comparison/[id]`)의 응답을 마크다운으로 렌더, 복사 버튼 추가.
- `globals.css` 에 `.md-body` 스타일(제목/목록/표/인라인코드/코드블록, 다크모드 대응).

## 의존성
- `react-markdown@9`, `remark-gfm@4`, `rehype-highlight@7`, `highlight.js@11`(CSS 테마 직접 import 위해 명시 추가).

## 검증 (라이브)
- FizzBuzz 비교(#3) 상세: 3 CLI 코드가 구문 하이라이트(키워드/문자열/숫자 색)로 렌더,
  인라인 코드(`Fizz`/`Buzz`) 배지, 굵은 글씨, 복사 버튼 노출 확인(스크린샷).
- `next build` 통과.

## 참고
- 블라인드 모드에서도 응답 본문은 마크다운 렌더(정체는 가림). 코드/표 비교 가독성↑.
