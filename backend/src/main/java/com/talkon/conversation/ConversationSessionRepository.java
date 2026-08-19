// 所有者付き会話検索と排他ロックを提供します。認可と同時更新の整合性をDB検索条件で守るRepositoryです。

package com.talkon.conversation;

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

/**
 * ConversationSessionRepositoryに関する責務をまとめるインターフェースです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。
 */
public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Long> {
  /** ユーザーの指定状態に一致する最新会話を取得します。 二重に会話を開始せず、進行中の会話を再利用するために必要です。 */
  Optional<ConversationSession> findFirstByUserIdAndStatusOrderByStartedAtDesc(
      Long userId, ConversationStatus status);

  /** ユーザーが所有する指定IDの会話を取得します。 他ユーザーの会話を参照できないよう所有者条件を常に適用するために必要です。 */
  Optional<ConversationSession> findByIdAndUserId(Long id, Long userId);

  /** ユーザーの会話履歴を新しい順のページとして取得します。 履歴が増えても必要な範囲だけ読み込むために必要です。 */
  Page<ConversationSession> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

  /** 指定日時以降に開始されたユーザーの会話を時系列で取得します。 ダッシュボードの対象期間だけを効率よく集計するために必要です。 */
  List<ConversationSession> findByUserIdAndStartedAtGreaterThanEqualOrderByStartedAtAsc(
      Long userId, Instant startedAt);

  /** ユーザーが所有する会話を更新用ロック付きで取得します。 同時リクエストによる発言順序や会話状態の競合を防ぐために必要です。 */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from ConversationSession s where s.id=:id and s.user.id=:userId")
  Optional<ConversationSession> lockOwned(@Param("id") Long id, @Param("userId") Long userId);
}
