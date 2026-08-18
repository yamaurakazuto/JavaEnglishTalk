// メール検索とユーザー排他取得を提供します。認証・会話処理からデータアクセスを分離するRepositoryです。

package com.talkon.user;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<User> findLockedById(Long id);
}
