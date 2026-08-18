// 会話順序・所有者認可・終了処理を統合検証します。主要会話フローとデータ分離を守るためのテストです。

package com.talkon;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talkon.auth.TalkOnPrincipal;
import com.talkon.conversation.ConversationMessageRepository;
import com.talkon.conversation.ConversationSessionRepository;
import com.talkon.feedback.ConversationFeedbackRepository;
import com.talkon.user.EnglishLevel;
import com.talkon.user.User;
import com.talkon.user.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
        .andExpect(jsonPath("$.llmUsage.inputTokens").value(0))
        .andExpect(jsonPath("$.llmUsage.outputTokens").value(0))
        .andExpect(jsonPath("$.llmUsage.estimatedCostMicros").value(0))
        .andExpect(jsonPath("$.llmUsage.model").value("local-llm"))
        .andExpect(
            jsonPath("$.messages[2].content")
                .value(org.hamcrest.Matchers.containsString("Where do you usually go?")));
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

  @Test
  void assistantWordCanBeTranslatedInItsConversationContext() throws Exception {
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
            post("/api/conversations/"
                    + conversationId
                    + "/messages/"
                    + messageId
                    + "/word-translation")
                .with(auth(owner))
                .with(csrf())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"word\":\"How\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.word").value("How"))
        .andExpect(jsonPath("$.translation").isNotEmpty());

    mvc.perform(
            post("/api/conversations/"
                    + conversationId
                    + "/messages/"
                    + messageId
                    + "/word-translation")
                .with(auth(owner))
                .with(csrf())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"word\":\"missing\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_WORD"));
  }

  @Test
  void voiceTurnTranscribesRepliesAndReturnsPlayableAudio() throws Exception {
    String json =
        mvc.perform(post("/api/conversations").with(auth(owner)).with(csrf()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long conversationId = new ObjectMapper().readTree(json).path("id").asLong();
    var audio = new MockMultipartFile("audio", "recording.webm", "audio/webm", new byte[1_024]);

    String result =
        mvc.perform(
                multipart("/api/conversations/" + conversationId + "/voice-turns")
                    .file(audio)
                    .with(auth(owner))
                    .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userTranscript").value("I like practicing English with TalkOn."))
            .andExpect(jsonPath("$.conversation.messages.length()").value(3))
            .andExpect(jsonPath("$.assistantAudioBase64").isNotEmpty())
            .andExpect(jsonPath("$.audioContentType").value("audio/wav"))
            .andExpect(jsonPath("$.processingTimes.sttMs").isNumber())
            .andExpect(jsonPath("$.processingTimes.llmMs").isNumber())
            .andExpect(jsonPath("$.processingTimes.ttsMs").isNumber())
            .andExpect(jsonPath("$.processingTimes.totalMs").isNumber())
            .andReturn()
            .getResponse()
            .getContentAsString();

    long messageId =
        new ObjectMapper()
            .readTree(result)
            .path("conversation")
            .path("messages")
            .path(2)
            .path("id")
            .asLong();
    mvc.perform(
            post("/api/conversations/" + conversationId + "/messages/" + messageId + "/speech")
                .with(auth(owner))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .string("Content-Type", "audio/wav"));
  }

  @Test
  void voiceTurnRejectsShortAndUnsupportedAudio() throws Exception {
    String json =
        mvc.perform(post("/api/conversations").with(auth(owner)).with(csrf()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long conversationId = new ObjectMapper().readTree(json).path("id").asLong();

    mvc.perform(
            multipart("/api/conversations/" + conversationId + "/voice-turns")
                .file(new MockMultipartFile("audio", "short.webm", "audio/webm", new byte[10]))
                .with(auth(owner))
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("AUDIO_TOO_SHORT"));

    mvc.perform(
            multipart("/api/conversations/" + conversationId + "/voice-turns")
                .file(new MockMultipartFile("audio", "bad.txt", "text/plain", new byte[1_024]))
                .with(auth(owner))
                .with(csrf()))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_AUDIO_TYPE"));
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
