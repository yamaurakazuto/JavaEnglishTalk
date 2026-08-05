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
      Long id, String role, String content, int sequenceNo, Instant createdAt) {}

  public record FeedbackResponse(
      String summary,
      JsonNode strengths,
      JsonNode improvements,
      JsonNode corrections,
      String overallComment,
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
        m.getId(), m.getRole().name(), m.getContent(), m.getSequenceNo(), m.getCreatedAt());
  }

  public static FeedbackResponse feedback(ConversationFeedback f, ObjectMapper m) {
    try {
      return new FeedbackResponse(
          f.getSummary(),
          m.readTree(f.getStrengths()),
          m.readTree(f.getImprovements()),
          m.readTree(f.getCorrections()),
          f.getOverallComment(),
          f.getCreatedAt());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
