// 会話開始と文脈付き応答生成だけを表すアプリケーション境界です。会話ユースケースを翻訳やFeedbackから分離します。

package com.talkon.conversation;

import com.talkon.user.EnglishLevel;
import java.util.List;

public interface ConversationAIService {
  AiResponse greeting(EnglishLevel level);

  AiResponse reply(List<ConversationMessage> messages, EnglishLevel level);

  record AiResponse(String text, int inputTokens, int outputTokens, String model) {}
}
