// ローカル自動ログイン用ユーザーを取得または作成します。ユーザー作成処理をFilterから分離するためのServiceです。

package com.kazuto.talkon.auth;

import com.kazuto.talkon.user.User;
import com.kazuto.talkon.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalAutoLoginService {
  static final String LOCAL_USER_EMAIL = "local@talkon.dev";

  private final UserRepository users;
  private final PasswordEncoder encoder;

  public LocalAutoLoginService(UserRepository users, PasswordEncoder encoder) {
    this.users = users;
    this.encoder = encoder;
  }

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
