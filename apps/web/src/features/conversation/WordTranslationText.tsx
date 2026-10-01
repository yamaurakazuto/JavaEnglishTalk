/**
 * AI英文を単語単位で表示し、ホバーまたはフォーカスした単語の日本語訳をツールチップで示します。
 */

import { useRef, useState } from "react";
import { api } from "../../shared/api";

type WordTranslationTextProps = {
  conversationId: number;
  messageId: number;
  content: string;
  onError: (message: string) => void;
};

type TooltipState = {
  key: string;
  text: string;
  loading: boolean;
};

const WORD_PATTERN = /([A-Za-z]+(?:['’][A-Za-z]+)*)/g;
const WORD_ONLY_PATTERN = /^[A-Za-z]+(?:['’][A-Za-z]+)*$/;
const HOVER_DELAY_MS = 300;

export function WordTranslationText({
  conversationId,
  messageId,
  content,
  onError,
}: WordTranslationTextProps) {
  const cache = useRef(new Map<string, string>());
  const timer = useRef<number | undefined>(undefined);
  const activeKey = useRef<string | undefined>(undefined);
  const [tooltip, setTooltip] = useState<TooltipState>();

  function showWord(word: string, index: number) {
    window.clearTimeout(timer.current);
    const key = `${index}-${word.toLowerCase()}`;
    activeKey.current = key;
    const cached = cache.current.get(word.toLowerCase());
    if (cached) {
      setTooltip({ key, text: cached, loading: false });
      return;
    }

    setTooltip({ key, text: "訳を調べています…", loading: true });
    timer.current = window.setTimeout(async () => {
      try {
        const result = await api.translateWord(conversationId, messageId, word);
        cache.current.set(word.toLowerCase(), result.translation);
        if (activeKey.current === key) {
          setTooltip({ key, text: result.translation, loading: false });
        }
      } catch (error) {
        if (activeKey.current === key) {
          setTooltip(undefined);
        }
        onError((error as Error).message);
      }
    }, HOVER_DELAY_MS);
  }

  function hideWord(key: string) {
    if (activeKey.current !== key) {
      return;
    }

    window.clearTimeout(timer.current);
    activeKey.current = undefined;
    setTooltip(undefined);
  }

  return (
    <p className="assistant-message-text">
      {content.split(WORD_PATTERN).map((part, index) => {
        if (!WORD_ONLY_PATTERN.test(part)) {
          return <span key={`${index}-${part}`}>{part}</span>;
        }

        const key = `${index}-${part.toLowerCase()}`;
        const tooltipId = `word-translation-${messageId}-${index}`;
        const visible = tooltip?.key === key;
        return (
          <span
            key={key}
            className="translatable-word"
            tabIndex={0}
            aria-describedby={visible ? tooltipId : undefined}
            onMouseEnter={() => showWord(part, index)}
            onMouseLeave={() => hideWord(key)}
            onFocus={() => showWord(part, index)}
            onBlur={() => hideWord(key)}
          >
            {part}
            {visible && (
              <span
                id={tooltipId}
                className="word-translation-tooltip"
                role="tooltip"
                aria-live="polite"
              >
                <strong>{part}</strong>
                <span>{tooltip.text}</span>
                {tooltip.loading && <span className="sr-only">読み込み中</span>}
              </span>
            )}
          </span>
        );
      })}
    </p>
  );
}
