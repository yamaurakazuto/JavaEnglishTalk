// 会話内の発言と順序を永続化します。ユーザーとAIの発言履歴を正確に復元するためのEntityです。

package com.talkon.conversation;

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
/** ConversationMessageに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
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

  @Column(columnDefinition = "TEXT")
  private String translation;

  @Column(name = "sequence_no", nullable = false)
  private int sequenceNo;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** ConversationMessageを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  protected ConversationMessage() {}

  /** ConversationMessageを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  public ConversationMessage(ConversationSession s, MessageRole r, String c, int n) {
    session = s;
    role = r;
    content = c;
    sequenceNo = n;
    createdAt = Instant.now();
  }

  /** get idとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public Long getId() {
    return id;
  }

  /** get roleとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public MessageRole getRole() {
    return role;
  }

  /** get contentとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public String getContent() {
    return content;
  }

  /** get translationとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public String getTranslation() {
    return translation;
  }

  /** translateの外部サービスまたは代替処理を実行します。 AI・音声機能の詳細を呼び出し側から分離し、実装を交換可能にするために必要です。 */
  public void translate(String translatedText) {
    translation = translatedText;
  }

  /** get sequence noとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public int getSequenceNo() {
    return sequenceNo;
  }

  /** get created atとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public Instant getCreatedAt() {
    return createdAt;
  }
}
