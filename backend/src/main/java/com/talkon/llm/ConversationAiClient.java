// 会話応答とフィードバック生成のLLM境界を定義します。外部APIを交換可能かつテスト可能にするPortです。

package com.talkon.llm;

import com.talkon.conversation.ConversationAIService;
import com.talkon.conversation.ConversationMessage;
import com.talkon.conversation.TranslationService;
import com.talkon.feedback.FeedbackData;
import java.util.List;

/** ConversationAiClientに関する責務をまとめるインターフェースです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public interface ConversationAiClient extends ConversationAIService, TranslationService {
  /** 会話履歴を分析して学習フィードバックを生成します。 会話・翻訳と同じAI接続を利用しながら、評価生成の契約を明示するために必要です。 */
  FeedbackData feedback(List<ConversationMessage> messages);
}
