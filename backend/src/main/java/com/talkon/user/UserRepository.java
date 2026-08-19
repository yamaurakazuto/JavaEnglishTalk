// メール検索とユーザー排他取得を提供します。認証・会話処理からデータアクセスを分離するRepositoryです。

package com.talkon.user;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** UserRepositoryに関する責務をまとめるインターフェースです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public interface UserRepository extends JpaRepository<User, Long> {
  /** 正規化済みメールアドレスに一致するユーザーを取得します。 ログイン時に認証対象のユーザーを特定するために必要です。 */
  Optional<User> findByEmail(String email);

  /** 指定メールアドレスが登録済みか確認します。 同じメールアドレスによる重複登録を防ぐために必要です。 */
  boolean existsByEmail(String email);

  /** ユーザーを更新用ロック付きで取得します。 同時更新によってプロフィール状態が競合することを防ぐために必要です。 */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<User> findLockedById(Long id);
}
