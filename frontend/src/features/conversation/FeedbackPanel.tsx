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
      {feedback.improvements.map((item, index) => (
        <article key={index}>
          <del>{item.original}</del>
          <strong>{item.suggestion}</strong>
          <p>{item.reason}</p>
        </article>
      ))}
      <h3>代表的な修正</h3>
      {feedback.corrections.map((item, index) => (
        <article key={index}>
          <del>{item.original}</del>
          <strong>{item.corrected}</strong>
          <p>{item.explanation}</p>
        </article>
      ))}
      <blockquote>{feedback.overallComment}</blockquote>
    </section>
  );
}
