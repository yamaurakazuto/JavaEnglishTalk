// フィードバック生成の状態を定義します。会話終了状態とAI処理状態を分離して扱うための列挙型です。

package com.talkon.feedback;

public enum FeedbackStatus {
  GENERATING,
  COMPLETED,
  FAILED
}
