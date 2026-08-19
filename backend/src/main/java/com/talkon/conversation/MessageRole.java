// 発言者の種類を定義します。ユーザー発言とAI発言を型安全に区別するための列挙型です。

package com.talkon.conversation;

/** MessageRoleに関する責務をまとめる列挙型です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public enum MessageRole {
  USER,
  ASSISTANT
}
