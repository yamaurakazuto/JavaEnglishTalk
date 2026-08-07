/**
 * フィードバックの生成中・成功・失敗を表示します。AI処理に失敗しても画面操作を続けられる構成です。
 */

import { Conversation } from "../../shared/api";

type FeedbackPanelProps = {
  conversation: Conversation;
  retrying: boolean;
  onRetry: () => void;
};

export function FeedbackPanel({
  conversation,
  retrying,
  onRetry,
}: FeedbackPanelProps) {
  const feedback = conversation.feedback;

  if (!feedback || feedback.status === "GENERATING") {
    return (
      <section className="feedback feedback-state" aria-live="polite">
        <h2>フィードバックを生成しています</h2>
        <p>会話は終了しました。この画面を開いたまま少しお待ちください。</p>
      </section>
    );
  }

  if (feedback.status === "FAILED") {
    return (
      <section className="feedback feedback-state" role="alert">
        <h2>フィードバックを生成できませんでした</h2>
        <p>
          {feedback.errorMessage ??
            "時間をおいて、もう一度フィードバックを生成してください。"}
        </p>
        <button type="button" onClick={onRetry} disabled={retrying}>
          {retrying ? "再生成しています…" : "もう一度試す"}
        </button>
      </section>
    );
  }

  return (
    <section className="feedback">
      <h2>Conversation feedback</h2>
      <p>{feedback.summary}</p>
      <h3>良かった点</h3>
      <ul>
        {feedback.strengths.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
      <h3>改善ポイント</h3>
      {feedback.corrections.length === 0 && (
        <p>明確に直す必要のある表現はありませんでした。</p>
      )}
      {feedback.corrections.map((item) => (
        <article
          className="correction-card"
          key={`${item.original}-${item.corrected}`}
        >
          <span className="category-label">{categoryName(item.category)}</span>
          <p className="original-expression">✕ {item.original}</p>
          <span className="correction-arrow">↓</span>
          <strong className="corrected-expression">✓ {item.corrected}</strong>
          <h4>💡 なぜ？</h4>
          <p>{item.reasonJa}</p>
          <small>別の表現</small>
          <p className="alternative-expression">{item.alternative}</p>
        </article>
      ))}
      {feedback.vocabularyTips.length > 0 && (
        <>
          <h3>覚えておきたいポイント</h3>
          <ul>
            {feedback.vocabularyTips.map((tip) => (
              <li key={tip}>{tip}</li>
            ))}
          </ul>
        </>
      )}
      <blockquote>{feedback.overallComment}</blockquote>
    </section>
  );
}

function categoryName(category: string) {
  const names: Record<string, string> = {
    GRAMMAR: "文法",
    VOCABULARY: "語彙",
    NATURALNESS: "自然な表現",
    WORD_ORDER: "語順",
  };
  return names[category] ?? category;
}
