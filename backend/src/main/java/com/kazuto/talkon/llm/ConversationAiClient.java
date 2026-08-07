// 会話応答とフィードバック生成のLLM境界を定義します。外部APIを交換可能かつテスト可能にするPortです。

package com.kazuto.talkon.llm;

import com.kazuto.talkon.conversation.ConversationMessage;
import com.kazuto.talkon.feedback.FeedbackData;
import com.kazuto.talkon.user.EnglishLevel;
import java.util.List;

public interface ConversationAiClient {
  String greeting(EnglishLevel level);

  String reply(List<ConversationMessage> messages, EnglishLevel level);

  String translate(String englishText);

  FeedbackData feedback(List<ConversationMessage> messages);
}
