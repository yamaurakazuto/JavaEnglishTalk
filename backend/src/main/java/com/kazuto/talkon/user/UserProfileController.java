// ログインユーザーの学習設定APIを提供します。初回レベル選択を認証処理から分離するControllerです。

package com.kazuto.talkon.user;

import com.kazuto.talkon.auth.AuthController.UserResponse;
import com.kazuto.talkon.auth.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {
  private final UserRepository users;

  public UserProfileController(UserRepository users) {
    this.users = users;
  }

  public record EnglishLevelRequest(@NotNull EnglishLevel englishLevel) {}

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
