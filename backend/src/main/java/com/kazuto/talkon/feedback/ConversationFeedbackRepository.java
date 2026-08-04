package com.kazuto.talkon.feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ConversationFeedbackRepository extends JpaRepository<ConversationFeedback,Long>{Optional<ConversationFeedback> findBySessionId(Long sessionId);boolean existsBySessionId(Long sessionId);}

