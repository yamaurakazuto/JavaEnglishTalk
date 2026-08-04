package com.kazuto.talkon.conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage,Long>{
 List<ConversationMessage> findBySessionIdOrderBySequenceNo(Long sessionId);
 long countBySessionIdAndRole(Long sessionId,MessageRole role);
}

