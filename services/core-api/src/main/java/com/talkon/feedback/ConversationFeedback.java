// 構造化フィードバックを会話単位で永続化します。一会話一件の学習結果を保持するためのEntityです。

package com.talkon.feedback;

import com.talkon.conversation.ConversationSession;
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

/** ConversationFeedbackに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
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

  /** ConversationFeedbackを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  protected ConversationFeedback() {}

  /** ConversationFeedbackを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  public ConversationFeedback(ConversationSession s) {
    session = s;
    status = FeedbackStatus.GENERATING;
    createdAt = Instant.now();
  }

  /** completeによって対象の状態や処理を更新します。 状態変更のルールを一箇所に集約し、不整合を防ぐために必要です。 */
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

  /** failによって対象の状態や処理を更新します。 状態変更のルールを一箇所に集約し、不整合を防ぐために必要です。 */
  public void fail() {
    status = FeedbackStatus.FAILED;
    errorMessage = "フィードバックの生成に失敗しました。もう一度試してください。";
  }

  /** retryによって対象の状態や処理を更新します。 状態変更のルールを一箇所に集約し、不整合を防ぐために必要です。 */
  public void retry() {
    status = FeedbackStatus.GENERATING;
    errorMessage = null;
  }

  /** get statusとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public FeedbackStatus getStatus() {
    return status;
  }

  /** get summaryとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public String getSummary() {
    return summary;
  }

  /** get strengthsとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public String getStrengths() {
    return strengths;
  }

  /** get improvementsとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public String getImprovements() {
    return improvements;
  }

  /** get correctionsとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public String getCorrections() {
    return corrections;
  }

  /** get vocabulary tipsとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public String getVocabularyTips() {
    return vocabularyTips;
  }

  /** get overall commentとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public String getOverallComment() {
    return overallComment;
  }

  /** get error messageとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public String getErrorMessage() {
    return errorMessage;
  }

  /** get created atとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public Instant getCreatedAt() {
    return createdAt;
  }
}
