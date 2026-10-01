// ログインユーザーのダッシュボード情報を返します。HTTP処理を集計ロジックから分けるためのControllerです。

package com.talkon.dashboard;

import com.talkon.auth.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** DashboardControllerに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
  private final DashboardService service;

  /** DashboardControllerを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  public DashboardController(DashboardService service) {
    this.service = service;
  }

  /** dashboardに対応する処理を実行します。 画面やHTTPリクエストから対象のユースケースを安全に利用できるようにするために必要です。 */
  @GetMapping
  public DashboardResponse dashboard(Authentication authentication) {
    return service.getDashboard(CurrentUser.require(authentication).id());
  }
}
