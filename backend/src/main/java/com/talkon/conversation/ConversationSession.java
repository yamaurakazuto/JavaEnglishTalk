// 会話の所有者・状態・開始終了日時を永続化します。会話ライフサイクルを明示的に管理するためのEntityです。

package com.talkon.conversation;

import com.talkon.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** ConversationSessionに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
@Entity
@Table(name = "conversation_sessions")
public class ConversationSession {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ConversationStatus status;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "llm_input_tokens", nullable = false)
  private long llmInputTokens;

  @Column(name = "llm_output_tokens", nullable = false)
  private long llmOutputTokens;

  @Column(name = "llm_cost_micros", nullable = false)
  private long llmCostMicros;

  @Column(name = "llm_model", length = 100)
  private String llmModel;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** ConversationSessionを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  protected ConversationSession() {}

  /** ConversationSessionを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  public ConversationSession(User user) {
    this.user = user;
    this.status = ConversationStatus.ACTIVE;
    this.startedAt = Instant.now();
    this.createdAt = startedAt;
    this.updatedAt = startedAt;
    this.llmInputTokens = 0;
    this.llmOutputTokens = 0;
    this.llmCostMicros = 0;
  }

  /** get idとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public Long getId() {
    return id;
  }

  /** get userとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public User getUser() {
    return user;
  }

  /** get statusとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public ConversationStatus getStatus() {
    return status;
  }

  /** get started atとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public Instant getStartedAt() {
    return startedAt;
  }

  /** get finished atとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public Instant getFinishedAt() {
    return finishedAt;
  }

  /** get llm input tokensとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public long getLlmInputTokens() {
    return llmInputTokens;
  }

  /** get llm output tokensとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public long getLlmOutputTokens() {
    return llmOutputTokens;
  }

  /** get llm cost microsとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public long getLlmCostMicros() {
    return llmCostMicros;
  }

  /** get llm modelとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public String getLlmModel() {
    return llmModel;
  }

  /** add llm usageによって対象の状態や処理を更新します。 状態変更のルールを一箇所に集約し、不整合を防ぐために必要です。 */
  public void addLlmUsage(int inputTokens, int outputTokens, long costMicros, String model) {
    llmInputTokens += Math.max(0, inputTokens);
    llmOutputTokens += Math.max(0, outputTokens);
    llmCostMicros += Math.max(0, costMicros);
    llmModel = model;
    updatedAt = Instant.now();
  }

  /** endによって対象の状態や処理を更新します。 状態変更のルールを一箇所に集約し、不整合を防ぐために必要です。 */
  public void end() {
    status = ConversationStatus.ENDED;
    finishedAt = Instant.now();
    updatedAt = finishedAt;
  }
}
