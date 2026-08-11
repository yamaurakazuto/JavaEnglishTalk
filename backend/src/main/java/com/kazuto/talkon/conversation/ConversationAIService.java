// 会話開始と文脈付き応答生成だけを表すアプリケーション境界です。会話ユースケースを翻訳やFeedbackから分離します。

package com.kazuto.talkon.conversation;

import com.kazuto.talkon.user.EnglishLevel;
import java.util.List;

public interface ConversationAIService {
  String greeting(EnglishLevel level);

  String reply(List<ConversationMessage> messages, EnglishLevel level);
}
