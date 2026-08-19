// 会話で許可する状態を定義します。状態遷移を文字列ではなく型で安全に扱うための列挙型です。

package com.talkon.conversation;

/** ConversationStatusに関する責務をまとめる列挙型です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public enum ConversationStatus {
  ACTIVE,
  ENDED
}
