// ブラウザへCSRFトークンを公開します。Cookie認証の更新系APIを安全に呼び出すために用意しています。

package com.talkon.auth;

import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** CsrfControllerに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
@RestController
public class CsrfController {
  /** csrfに対応する処理を実行します。 画面やHTTPリクエストから対象のユースケースを安全に利用できるようにするために必要です。 */
  @GetMapping("/api/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }
}
