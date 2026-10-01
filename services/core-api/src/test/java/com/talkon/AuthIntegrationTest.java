// 登録・ログイン・セッション認証を統合検証します。認証の主要な安全要件を回帰テストで守るためのテストです。

package com.talkon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.talkon.user.User;
import com.talkon.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  @Autowired PasswordEncoder encoder;

  @Test
  void registrationHashesPasswordAndRejectsDuplicate() throws Exception {
    String body =
        "{\"displayName\":\" Test User"
            + " \",\"email\":\"TEST@example.com\",\"password\":\"password123\"}";
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());
    var saved = users.findByEmail("test@example.com").orElseThrow();
    assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
    assertThat(encoder.matches("password123", saved.getPasswordHash())).isTrue();
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void protectedApiReturns401() {
    try {
      mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void loginCreatesSessionAndBadPasswordIsRejected() throws Exception {
    users.save(new User("Login User", "login@example.com", encoder.encode("password123")));
    var login =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"login@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andReturn();
    mvc.perform(get("/api/auth/me").session((MockHttpSession) login.getRequest().getSession(false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("login@example.com"));
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"login@example.com\",\"password\":\"wrong-password\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void englishLevelUpdateAllowsCorsPreflight() throws Exception {
    mvc.perform(
            options("/api/users/me/english-level")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "PUT")
                .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
        .andExpect(status().isOk());
  }

  @Test
  void englishLevelIsSavedAndReturnedForCurrentUser() throws Exception {
    users.save(new User("Level User", "level@example.com", encoder.encode("password123")));
    var login =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"level@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andReturn();
    var session = (MockHttpSession) login.getRequest().getSession(false);

    mvc.perform(
            put("/api/users/me/english-level")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"englishLevel\":\"ADVANCED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.englishLevel").value("ADVANCED"));
    mvc.perform(get("/api/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.englishLevel").value("ADVANCED"));
  }
}
