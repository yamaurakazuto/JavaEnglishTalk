// 会話フィードバックの検索と重複確認を提供します。永続化操作を生成ロジックから分離するRepositoryです。

package com.kazuto.talkon.feedback;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationFeedbackRepository extends JpaRepository<ConversationFeedback, Long> {
  Optional<ConversationFeedback> findBySessionId(Long sessionId);

  boolean existsBySessionId(Long sessionId);
}
