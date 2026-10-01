// フィードバック生成の状態を定義します。会話終了状態とAI処理状態を分離して扱うための列挙型です。

package com.talkon.feedback;

/** FeedbackStatusに関する責務をまとめる列挙型です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public enum FeedbackStatus {
  GENERATING,
  COMPLETED,
  FAILED
}
