// 会話応答とフィードバック生成のLLM境界を定義します。外部APIを交換可能かつテスト可能にするPortです。

package com.kazuto.talkon.llm;

import com.kazuto.talkon.conversation.ConversationAIService;
import com.kazuto.talkon.conversation.ConversationMessage;
import com.kazuto.talkon.conversation.TranslationService;
import com.kazuto.talkon.feedback.FeedbackData;
import java.util.List;

public interface ConversationAiClient extends ConversationAIService, TranslationService {
  FeedbackData feedback(List<ConversationMessage> messages);
}
