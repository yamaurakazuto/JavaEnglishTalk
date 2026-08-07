// 会話APIのレスポンス型と変換処理を定義します。永続化Entityを外部へ直接公開しないためのDTO群です。

package com.kazuto.talkon.conversation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazuto.talkon.feedback.ConversationFeedback;
import java.time.Instant;
import java.util.List;

public final class ConversationDtos {
  private ConversationDtos() {}

  public record MessageResponse(
      Long id,
      String role,
      String content,
      String translation,
      int sequenceNo,
      Instant createdAt) {}

  public record FeedbackResponse(
      String status,
      String summary,
      JsonNode strengths,
      JsonNode improvements,
      JsonNode corrections,
      String overallComment,
      String errorMessage,
      Instant createdAt) {}

  public record Detail(
      Long id,
      String status,
      Instant startedAt,
      Instant finishedAt,
      List<MessageResponse> messages,
      FeedbackResponse feedback) {}

  public record Summary(Long id, String status, Instant startedAt, Instant finishedAt) {}

  public record PageResponse(
      List<Summary> content, int page, int size, long totalElements, int totalPages) {}

  public static MessageResponse message(ConversationMessage m) {
    return new MessageResponse(
        m.getId(),
        m.getRole().name(),
        m.getContent(),
        m.getTranslation(),
        m.getSequenceNo(),
        m.getCreatedAt());
  }

  public static FeedbackResponse feedback(ConversationFeedback f, ObjectMapper m) {
    try {
      return new FeedbackResponse(
          f.getStatus().name(),
          f.getSummary(),
          jsonOrEmptyArray(f.getStrengths(), m),
          jsonOrEmptyArray(f.getImprovements(), m),
          jsonOrEmptyArray(f.getCorrections(), m),
          f.getOverallComment(),
          f.getErrorMessage(),
          f.getCreatedAt());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static JsonNode jsonOrEmptyArray(String value, ObjectMapper mapper) throws Exception {
    if (value == null) {
      return mapper.createArrayNode();
    }
    JsonNode parsed = mapper.readTree(value);
    return parsed.isTextual() ? mapper.readTree(parsed.asText()) : parsed;
  }
}
