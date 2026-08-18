// ログインユーザーのダッシュボード情報を返します。HTTP処理を集計ロジックから分けるためのControllerです。

package com.talkon.dashboard;

import com.talkon.auth.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
  private final DashboardService service;

  public DashboardController(DashboardService service) {
    this.service = service;
  }

  @GetMapping
  public DashboardResponse dashboard(Authentication authentication) {
    return service.getDashboard(CurrentUser.require(authentication).id());
  }
}
