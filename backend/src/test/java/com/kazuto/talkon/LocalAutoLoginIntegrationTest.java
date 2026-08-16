// ローカル自動ログインの有効範囲を統合検証します。外部接続で認証が省略される事故を防ぐためのテストです。

package com.kazuto.talkon;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.local-auto-login=true")
@AutoConfigureMockMvc
class LocalAutoLoginIntegrationTest {
  @Autowired MockMvc mvc;

  @Test
  void loopbackRequestUsesLocalDevelopmentUser() throws Exception {
    mvc.perform(get("/api/auth/me").with(request -> setRemoteAddress(request, "127.0.0.1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("local@talkon.dev"));
  }

  @Test
  void nonLoopbackRequestStillRequiresAuthentication() throws Exception {
    mvc.perform(get("/api/auth/me").with(request -> setRemoteAddress(request, "192.0.2.10")))
        .andExpect(status().isUnauthorized());
  }

  private static org.springframework.mock.web.MockHttpServletRequest setRemoteAddress(
      org.springframework.mock.web.MockHttpServletRequest request, String remoteAddress) {
    request.setRemoteAddr(remoteAddress);
    return request;
  }
}
