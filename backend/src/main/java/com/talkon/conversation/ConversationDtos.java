// 会話APIのレスポンス型と変換処理を定義します。永続化Entityを外部へ直接公開しないためのDTO群です。

package com.talkon.conversation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talkon.feedback.ConversationFeedback;
import java.time.Instant;
import java.util.List;

/** ConversationDtosに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public final class ConversationDtos {
  /** ConversationDtosを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  private ConversationDtos() {}

  /** MessageResponseに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  public record MessageResponse(
      Long id,
      String role,
      String content,
      String translation,
      int sequenceNo,
      Instant createdAt) {}

  /** WordTranslationResponseに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  public record WordTranslationResponse(String word, String translation) {}

  /** FeedbackResponseに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  public record FeedbackResponse(
      String status,
      String summary,
      JsonNode strengths,
      JsonNode corrections,
      JsonNode vocabularyTips,
      String overallComment,
      String errorMessage,
      Instant createdAt) {}

  /** Detailに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  public record Detail(
      Long id,
      String status,
      Instant startedAt,
      Instant finishedAt,
      List<MessageResponse> messages,
      FeedbackResponse feedback,
      LlmUsageResponse llmUsage) {}

  /** LlmUsageResponseに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  public record LlmUsageResponse(
      long inputTokens, long outputTokens, long estimatedCostMicros, String model) {}

  /** Summaryに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  public record Summary(Long id, String status, Instant startedAt, Instant finishedAt) {}

  /** PageResponseに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  public record PageResponse(
      List<Summary> content, int page, int size, long totalElements, int totalPages) {}

  /** messageに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  public static MessageResponse message(ConversationMessage m) {
    return new MessageResponse(
        m.getId(),
        m.getRole().name(),
        m.getContent(),
        m.getTranslation(),
        m.getSequenceNo(),
        m.getCreatedAt());
  }

  /** feedbackの外部サービスまたは代替処理を実行します。 AI・音声機能の詳細を呼び出し側から分離し、実装を交換可能にするために必要です。 */
  public static FeedbackResponse feedback(ConversationFeedback f, ObjectMapper m) {
    try {
      return new FeedbackResponse(
          f.getStatus().name(),
          f.getSummary(),
          jsonOrEmptyArray(f.getStrengths(), m),
          jsonOrEmptyArray(f.getCorrections(), m),
          jsonOrEmptyArray(f.getVocabularyTips(), m),
          f.getOverallComment(),
          f.getErrorMessage(),
          f.getCreatedAt());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /** json or empty arrayに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  private static JsonNode jsonOrEmptyArray(String value, ObjectMapper mapper) throws Exception {
    if (value == null) {
      return mapper.createArrayNode();
    }
    JsonNode parsed = mapper.readTree(value);
    return parsed.isTextual() ? mapper.readTree(parsed.asText()) : parsed;
  }
}
