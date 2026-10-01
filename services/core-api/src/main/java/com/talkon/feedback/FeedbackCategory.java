// 学習上の修正理由を分類します。Feedback画面で改善ポイントを理解しやすくする列挙型です。

package com.talkon.feedback;

/** FeedbackCategoryに関する責務をまとめる列挙型です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public enum FeedbackCategory {
  GRAMMAR,
  VOCABULARY,
  NATURALNESS,
  WORD_ORDER
}
