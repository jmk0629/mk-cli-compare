"use client";

import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeHighlight from "rehype-highlight";
import "highlight.js/styles/github.css";

/** CLI 응답(마크다운/코드)을 사람이 읽기 좋게 렌더. GFM 표·체크박스 + 코드 하이라이트. */
export default function Markdown({ content }: { content: string }) {
  return (
    <div className="md-body">
      <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[[rehypeHighlight, { detect: true, ignoreMissing: true }]]}>
        {content}
      </ReactMarkdown>
    </div>
  );
}
