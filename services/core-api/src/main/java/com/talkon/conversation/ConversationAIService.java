// 会話開始と文脈付き応答生成だけを表すアプリケーション境界です。会話ユースケースを翻訳やFeedbackから分離します。

package com.talkon.conversation;

import com.talkon.user.EnglishLevel;
import java.util.List;

/** ConversationAIServiceに関する責務をまとめるインターフェースです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public interface ConversationAIService {
  /** 学習レベルに合う最初のAIメッセージを生成します。 会話開始処理を特定のAI実装へ依存させず、差し替え可能にするために必要です。 */
  AiResponse greeting(EnglishLevel level);

  /** これまでの会話履歴と学習レベルを基にAIの返答を生成します。 会話進行処理とAIサービスへの接続処理を分離するために必要です。 */
  AiResponse reply(List<ConversationMessage> messages, EnglishLevel level);

  /** AiResponseに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  record AiResponse(String text, int inputTokens, int outputTokens, String model) {}
}
