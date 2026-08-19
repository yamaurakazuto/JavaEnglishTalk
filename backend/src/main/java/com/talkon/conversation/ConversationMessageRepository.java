// 会話メッセージの検索・件数取得を提供します。データアクセスを会話ロジックから分離するためのRepositoryです。

package com.talkon.conversation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ConversationMessageRepositoryに関する責務をまとめるインターフェースです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。
 */
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
  /** 指定した会話のメッセージを発言順に取得します。 会話画面とAIへ正しい順序の履歴を渡すために必要です。 */
  List<ConversationMessage> findBySessionIdOrderBySequenceNo(Long sessionId);

  /** 指定した会話と発言者に一致するメッセージ数を数えます。 会話の利用状況を役割別に集計するために必要です。 */
  long countBySessionIdAndRole(Long sessionId, MessageRole role);

  /** 指定した会話に保存された全メッセージ数を数えます。 次の発言順序や会話量を判定するために必要です。 */
  long countBySessionId(Long sessionId);

  /** 会話IDとメッセージIDの両方に一致するメッセージを取得します。 別の会話に属するメッセージへ誤ってアクセスすることを防ぐために必要です。 */
  Optional<ConversationMessage> findByIdAndSessionId(Long id, Long sessionId);
}
