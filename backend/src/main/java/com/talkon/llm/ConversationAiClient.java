// 会話応答とフィードバック生成のLLM境界を定義します。外部APIを交換可能かつテスト可能にするPortです。

package com.talkon.llm;

import com.talkon.conversation.ConversationAIService;
import com.talkon.conversation.ConversationMessage;
import com.talkon.conversation.TranslationService;
import com.talkon.feedback.FeedbackData;
import java.util.List;

public interface ConversationAiClient extends ConversationAIService, TranslationService {
  FeedbackData feedback(List<ConversationMessage> messages);
}
