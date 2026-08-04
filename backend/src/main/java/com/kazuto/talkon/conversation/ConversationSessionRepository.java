package com.kazuto.talkon.conversation;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface ConversationSessionRepository extends JpaRepository<ConversationSession,Long>{
 Optional<ConversationSession> findFirstByUserIdAndStatusOrderByStartedAtDesc(Long userId,ConversationStatus status);
 Optional<ConversationSession> findByIdAndUserId(Long id,Long userId);
 Page<ConversationSession> findByUserIdOrderByStartedAtDesc(Long userId,Pageable pageable);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from ConversationSession s where s.id=:id and s.user.id=:userId")
 Optional<ConversationSession> lockOwned(@Param("id") Long id,@Param("userId") Long userId);
}

