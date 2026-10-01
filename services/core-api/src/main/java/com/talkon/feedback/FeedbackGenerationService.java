// 終了済み会話のフィードバックを非同期生成します。AI障害が会話終了を失敗させないためのServiceです。

package com.talkon.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talkon.conversation.ConversationMessageRepository;
import com.talkon.llm.ConversationAiClient;
import jakarta.validation.Validator;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** FeedbackGenerationServiceに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
@Service
public class FeedbackGenerationService {
  private static final Logger log = LoggerFactory.getLogger(FeedbackGenerationService.class);

  private final ConversationMessageRepository messages;
  private final ConversationFeedbackRepository feedbacks;
  private final ConversationAiClient ai;
  private final ObjectMapper mapper;
  private final Validator validator;
  private final TransactionTemplate tx;

  /** FeedbackGenerationServiceを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  public FeedbackGenerationService(
      ConversationMessageRepository messages,
      ConversationFeedbackRepository feedbacks,
      ConversationAiClient ai,
      ObjectMapper mapper,
      Validator validator,
      TransactionTemplate tx) {
    this.messages = messages;
    this.feedbacks = feedbacks;
    this.ai = ai;
    this.mapper = mapper;
    this.validator = validator;
    this.tx = tx;
  }

  /** generateの外部サービスまたは代替処理を実行します。 AI・音声機能の詳細を呼び出し側から分離し、実装を交換可能にするために必要です。 */
  @Async
  public void generate(Long conversationId, Long userId) {
    Instant startedAt = Instant.now();
    try {
      FeedbackData data = createFeedback(conversationId);
      tx.executeWithoutResult(
          ignored ->
              feedbacks.findBySessionId(conversationId).orElseThrow().complete(data, mapper));
      log.info(
          "userId={} conversationId={} action=generateFeedback status=COMPLETED durationMs={}",
          userId,
          conversationId,
          Duration.between(startedAt, Instant.now()).toMillis());
    } catch (Exception exception) {
      tx.executeWithoutResult(
          ignored ->
              feedbacks.findBySessionId(conversationId).ifPresent(ConversationFeedback::fail));
      log.error(
          "userId={} conversationId={} action=generateFeedback status=FAILED durationMs={}",
          userId,
          conversationId,
          Duration.between(startedAt, Instant.now()).toMillis(),
          exception);
    }
  }

  /** create feedbackに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  private FeedbackData createFeedback(Long conversationId) {
    for (int attempt = 1; attempt <= 2; attempt++) {
      try {
        FeedbackData candidate =
            ai.feedback(messages.findBySessionIdOrderBySequenceNo(conversationId));
        if (validator.validate(candidate).isEmpty()) {
          return candidate;
        }
      } catch (Exception exception) {
        log.warn(
            "conversationId={} action=generateFeedback attempt={} status=RETRY",
            conversationId,
            attempt,
            exception);
      }
    }
    throw new IllegalStateException("Feedback generation failed after retry");
  }
}
