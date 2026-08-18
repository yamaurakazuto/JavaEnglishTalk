// ブラウザへCSRFトークンを公開します。Cookie認証の更新系APIを安全に呼び出すために用意しています。

package com.talkon.auth;

import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {
  @GetMapping("/api/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }
}
