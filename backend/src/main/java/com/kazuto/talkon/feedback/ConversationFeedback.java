// 構造化フィードバックを会話単位で永続化します。一会話一件の学習結果を保持するためのEntityです。

package com.kazuto.talkon.feedback;

import com.kazuto.talkon.conversation.ConversationSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "conversation_feedbacks")
public class ConversationFeedback {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", unique = true)
  private ConversationSession session;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FeedbackStatus status;

  @Column(columnDefinition = "TEXT")
  private String summary;

  @Column(columnDefinition = "JSON")
  private String strengths;

  @Column(columnDefinition = "JSON")
  private String improvements;

  @Column(columnDefinition = "JSON")
  private String corrections;

  @Column(name = "vocabulary_tips", columnDefinition = "JSON")
  private String vocabularyTips;

  @Column(name = "overall_comment", columnDefinition = "TEXT")
  private String overallComment;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ConversationFeedback() {}

  public ConversationFeedback(ConversationSession s) {
    session = s;
    status = FeedbackStatus.GENERATING;
    createdAt = Instant.now();
  }

  public void complete(FeedbackData data, com.fasterxml.jackson.databind.ObjectMapper mapper) {
    summary = data.summary();
    overallComment = data.overallComment();
    try {
      strengths = mapper.writeValueAsString(data.strengths());
      improvements = "[]";
      corrections = mapper.writeValueAsString(data.corrections());
      vocabularyTips = mapper.writeValueAsString(data.vocabularyTips());
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
    status = FeedbackStatus.COMPLETED;
    errorMessage = null;
  }

  public void fail() {
    status = FeedbackStatus.FAILED;
    errorMessage = "フィードバックの生成に失敗しました。もう一度試してください。";
  }

  public void retry() {
    status = FeedbackStatus.GENERATING;
    errorMessage = null;
  }

  public FeedbackStatus getStatus() {
    return status;
  }

  public String getSummary() {
    return summary;
  }

  public String getStrengths() {
    return strengths;
  }

  public String getImprovements() {
    return improvements;
  }

  public String getCorrections() {
    return corrections;
  }

  public String getVocabularyTips() {
    return vocabularyTips;
  }

  public String getOverallComment() {
    return overallComment;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
