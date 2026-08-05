// 会話内の発言と順序を永続化します。ユーザーとAIの発言履歴を正確に復元するためのEntityです。

package com.kazuto.talkon.conversation;

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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "conversation_messages",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_messages_sequence",
            columnNames = {"session_id", "sequence_no"}))
public class ConversationMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id")
  private ConversationSession session;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MessageRole role;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "sequence_no", nullable = false)
  private int sequenceNo;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ConversationMessage() {}

  public ConversationMessage(ConversationSession s, MessageRole r, String c, int n) {
    session = s;
    role = r;
    content = c;
    sequenceNo = n;
    createdAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public MessageRole getRole() {
    return role;
  }

  public String getContent() {
    return content;
  }

  public int getSequenceNo() {
    return sequenceNo;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
