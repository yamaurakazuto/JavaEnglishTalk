// 発言者の種類を定義します。ユーザー発言とAI発言を型安全に区別するための列挙型です。

package com.talkon.conversation;

public enum MessageRole {
  USER,
  ASSISTANT
}
