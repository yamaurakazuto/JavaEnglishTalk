// ログインユーザーの学習設定APIを提供します。初回レベル選択を認証処理から分離するControllerです。

package com.talkon.user;

import com.talkon.auth.AuthController.UserResponse;
import com.talkon.auth.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UserProfileControllerに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {
  private final UserRepository users;

  /** UserProfileControllerを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  public UserProfileController(UserRepository users) {
    this.users = users;
  }

  /** EnglishLevelRequestに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  public record EnglishLevelRequest(@NotNull EnglishLevel englishLevel) {}

  /** select levelによって対象の状態や処理を更新します。 状態変更のルールを一箇所に集約し、不整合を防ぐために必要です。 */
  @PutMapping("/english-level")
  @Transactional
  public UserResponse selectLevel(
      @Valid @RequestBody EnglishLevelRequest request, Authentication authentication) {
    var user = users.findById(CurrentUser.require(authentication).id()).orElseThrow();
    user.selectEnglishLevel(request.englishLevel());
    return new UserResponse(
        user.getId(), user.getDisplayName(), user.getEmail(), user.getEnglishLevel());
  }
}
