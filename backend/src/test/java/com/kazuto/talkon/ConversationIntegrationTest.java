// 会話順序・所有者認可・終了処理を統合検証します。主要会話フローとデータ分離を守るためのテストです。

package com.kazuto.talkon;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazuto.talkon.auth.TalkOnPrincipal;
import com.kazuto.talkon.conversation.ConversationMessageRepository;
import com.kazuto.talkon.conversation.ConversationSessionRepository;
import com.kazuto.talkon.feedback.ConversationFeedbackRepository;
import com.kazuto.talkon.user.EnglishLevel;
import com.kazuto.talkon.user.User;
import com.kazuto.talkon.user.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class ConversationIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  @Autowired ConversationFeedbackRepository feedbacks;
  @Autowired ConversationMessageRepository messages;
  @Autowired ConversationSessionRepository sessions;
  @Autowired PasswordEncoder encoder;
  User owner;
  User other;

  @BeforeEach
  void setup() {
    feedbacks.deleteAll();
    messages.deleteAll();
    sessions.deleteAll();
    users.deleteAll();
    owner = users.save(new User("Owner", "owner@example.com", encoder.encode("password123")));
    other = users.save(new User("Other", "other@example.com", encoder.encode("password123")));
    owner.selectEnglishLevel(EnglishLevel.BEGINNER);
    other.selectEnglishLevel(EnglishLevel.INTERMEDIATE);
    users.saveAll(List.of(owner, other));
  }

  private RequestPostProcessor auth(User u) {
    var p = new TalkOnPrincipal(u.getId(), u.getEmail(), u.getDisplayName());
    return authentication(
        UsernamePasswordAuthenticationToken.authenticated(
            p, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
  }

  @Test
  void conversationIsOrderedAndOwnershipIsHidden() throws Exception {
    String json =
        mvc.perform(post("/api/conversations").with(auth(owner)).with(csrf()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = new ObjectMapper().readTree(json).path("id").asLong();
    org.assertj.core.api.Assertions.assertThat(
            new ObjectMapper().readTree(json).path("messages").path(0).path("content").asText())
        .isEqualTo("Hi! It's nice to meet you. How are you today?");
    mvc.perform(
            post("/api/conversations/" + id + "/messages")
                .with(auth(owner))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"I like hiking.\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messages[1].role").value("USER"))
        .andExpect(jsonPath("$.messages[2].role").value("ASSISTANT"))
        .andExpect(
            jsonPath("$.messages[2].content")
                .value(org.hamcrest.Matchers.containsString("Oh, I see")));
    mvc.perform(get("/api/conversations/" + id).with(auth(other))).andExpect(status().isNotFound());
    mvc.perform(post("/api/conversations/" + id + "/finish").with(auth(owner)).with(csrf()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("ENDED"))
        .andExpect(jsonPath("$.feedback.status").exists());
    waitForCompletedFeedback(id);
    mvc.perform(get("/api/conversations/" + id).with(auth(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.feedback.strengths").isArray())
        .andExpect(jsonPath("$.feedback.corrections").isArray())
        .andExpect(jsonPath("$.feedback.vocabularyTips").isArray());
    mvc.perform(get("/api/dashboard").with(auth(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.todayStudySeconds").isNumber())
        .andExpect(jsonPath("$.totalStudyDays").value(1))
        .andExpect(
            jsonPath("$.activities[?(@.sessionCount == 1)].messageCount")
                .value(org.hamcrest.Matchers.hasItem(3)))
        .andExpect(jsonPath("$.activities").isArray());
    mvc.perform(
            post("/api/conversations/" + id + "/messages")
                .with(auth(owner))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"again\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void assistantMessageCanBeTranslatedWithoutChangingConversation() throws Exception {
    String json =
        mvc.perform(post("/api/conversations").with(auth(owner)).with(csrf()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var conversation = new ObjectMapper().readTree(json);
    long conversationId = conversation.path("id").asLong();
    long messageId = conversation.path("messages").path(0).path("id").asLong();

    mvc.perform(
            post("/api/conversations/" + conversationId + "/messages/" + messageId + "/translation")
                .with(auth(owner))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ASSISTANT"))
        .andExpect(jsonPath("$.translation").isNotEmpty());
  }

  private void waitForCompletedFeedback(long id) throws Exception {
    for (int attempt = 0; attempt < 20; attempt++) {
      String body =
          mvc.perform(get("/api/conversations/" + id).with(auth(owner)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();
      if ("COMPLETED"
          .equals(new ObjectMapper().readTree(body).path("feedback").path("status").asText())) {
        return;
      }
      Thread.sleep(50);
    }
    throw new AssertionError("Feedback did not complete");
  }
}
