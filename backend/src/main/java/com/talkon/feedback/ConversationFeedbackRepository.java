// 会話フィードバックの検索と重複確認を提供します。永続化操作を生成ロジックから分離するRepositoryです。

package com.talkon.feedback;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ConversationFeedbackRepositoryに関する責務をまとめるインターフェースです。
 * 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。
 */
public interface ConversationFeedbackRepository extends JpaRepository<ConversationFeedback, Long> {
  /** 指定した会話に対応するフィードバックを取得します。 会話詳細へ生成済みの評価を表示するために必要です。 */
  Optional<ConversationFeedback> findBySessionId(Long sessionId);

  /** 指定した会話のフィードバックが存在するか確認します。 同じ会話へフィードバックを重複作成しないために必要です。 */
  boolean existsBySessionId(Long sessionId);
}
