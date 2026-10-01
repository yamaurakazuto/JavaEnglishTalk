// Spring Securityの認証情報から現在の利用者を取得します。所有者判定を共通化するためのヘルパーです。

package com.talkon.auth;

import com.talkon.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

/** CurrentUserに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public final class CurrentUser {
  /** CurrentUserを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  private CurrentUser() {}

  /** requireに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  public static TalkOnPrincipal require(Authentication auth) {
    if (auth == null || !(auth.getPrincipal() instanceof TalkOnPrincipal p)) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "ログインが必要です。");
    }
    return p;
  }
}
