// 所有者付き会話検索と排他ロックを提供します。認可と同時更新の整合性をDB検索条件で守るRepositoryです。

package com.kazuto.talkon.conversation;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Long> {
  Optional<ConversationSession> findFirstByUserIdAndStatusOrderByStartedAtDesc(
      Long userId, ConversationStatus status);

  Optional<ConversationSession> findByIdAndUserId(Long id, Long userId);

  Page<ConversationSession> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

  List<ConversationSession> findByUserIdAndStartedAtGreaterThanEqualOrderByStartedAtAsc(
      Long userId, Instant startedAt);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from ConversationSession s where s.id=:id and s.user.id=:userId")
  Optional<ConversationSession> lockOwned(@Param("id") Long id, @Param("userId") Long userId);
}
