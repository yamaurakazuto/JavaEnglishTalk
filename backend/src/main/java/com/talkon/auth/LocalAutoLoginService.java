// ローカル自動ログイン用ユーザーを取得または作成します。ユーザー作成処理をFilterから分離するためのServiceです。

package com.talkon.auth;

import com.talkon.user.User;
import com.talkon.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** LocalAutoLoginServiceに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
@Service
public class LocalAutoLoginService {
  static final String LOCAL_USER_EMAIL = "local@talkon.dev";

  private final UserRepository users;
  private final PasswordEncoder encoder;

  /** LocalAutoLoginServiceを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  public LocalAutoLoginService(UserRepository users, PasswordEncoder encoder) {
    this.users = users;
    this.encoder = encoder;
  }

  /** get or create userとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  @Transactional
  public User getOrCreateUser() {
    return users
        .findByEmail(LOCAL_USER_EMAIL)
        .orElseGet(
            () ->
                users.save(
                    new User(
                        "Local User",
                        LOCAL_USER_EMAIL,
                        encoder.encode("local-auto-login-disabled-password"))));
  }
}
