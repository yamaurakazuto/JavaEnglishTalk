// 会話メッセージの検索・件数取得を提供します。データアクセスを会話ロジックから分離するためのRepositoryです。

package com.kazuto.talkon.conversation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
  List<ConversationMessage> findBySessionIdOrderBySequenceNo(Long sessionId);

  long countBySessionIdAndRole(Long sessionId, MessageRole role);
}
