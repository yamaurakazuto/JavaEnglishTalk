// 会話で許可する状態を定義します。状態遷移を文字列ではなく型で安全に扱うための列挙型です。

package com.talkon.conversation;

public enum ConversationStatus {
  ACTIVE,
  ENDED
}
