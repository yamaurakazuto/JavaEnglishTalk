// 会話の所有者・状態・開始終了日時を永続化します。会話ライフサイクルを明示的に管理するためのEntityです。

package com.kazuto.talkon.conversation;

import com.kazuto.talkon.user.User;
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

  protected ConversationSession() {}

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

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public ConversationStatus getStatus() {
    return status;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }

  public long getLlmInputTokens() {
    return llmInputTokens;
  }

  public long getLlmOutputTokens() {
    return llmOutputTokens;
  }

  public long getLlmCostMicros() {
    return llmCostMicros;
  }

  public String getLlmModel() {
    return llmModel;
  }

  public void addLlmUsage(int inputTokens, int outputTokens, long costMicros, String model) {
    llmInputTokens += Math.max(0, inputTokens);
    llmOutputTokens += Math.max(0, outputTokens);
    llmCostMicros += Math.max(0, costMicros);
    llmModel = model;
    updatedAt = Instant.now();
  }

  public void end() {
    status = ConversationStatus.ENDED;
    finishedAt = Instant.now();
    updatedAt = finishedAt;
  }
}
