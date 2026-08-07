// FakeまたはOpenAI互換LLMクライアントを構成します。環境に応じた実装切替を会話機能から隠蔽する設定です。

package com.kazuto.talkon.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazuto.talkon.conversation.ConversationMessage;
import com.kazuto.talkon.conversation.MessageRole;
import com.kazuto.talkon.feedback.FeedbackData;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiClientConfig {
  @Bean
  ConversationAiClient client(
      ObjectMapper mapper,
      @Value("${app.llm.api-key:}") String key,
      @Value("${app.llm.base-url}") String url,
      @Value("${app.llm.model}") String model,
      @Value("${app.llm.timeout-seconds:30}") int timeout) {
    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(timeout));
    requestFactory.setReadTimeout(Duration.ofSeconds(timeout));
    return key.isBlank()
        ? new LocalAiClient()
        : new OpenAiClient(
            mapper,
            RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                .build(),
            model);
  }

  static class LocalAiClient implements ConversationAiClient {
    public String greeting() {
      return "Hi! I'm your English conversation partner. How was your day?";
    }

    public String reply(List<ConversationMessage> m) {
      String latest =
          m.stream()
              .filter(x -> x.getRole() == MessageRole.USER)
              .reduce((first, second) -> second)
              .map(ConversationMessage::getContent)
              .orElse("that");
      long turn = m.stream().filter(x -> x.getRole() == MessageRole.USER).count();
      var questions =
          List.of(
              "How did that make you feel?",
              "What happened next?",
              "Would you like to do that again?");
      String shortLatest = latest.length() > 80 ? latest.substring(0, 80) + "..." : latest;
      return "Thanks for sharing. You mentioned: "
          + shortLatest
          + " "
          + questions.get((int) ((turn - 1) % questions.size()));
    }

    public String translate(String englishText) {
      return "この英文は「" + englishText + "」という内容です。";
    }

    public FeedbackData feedback(List<ConversationMessage> m) {
      var text =
          m.stream()
              .filter(x -> x.getRole() == MessageRole.USER)
              .findFirst()
              .map(ConversationMessage::getContent)
              .orElse("Your message");
      return new FeedbackData(
          "You practiced a friendly everyday conversation.",
          List.of("You kept the conversation moving."),
          List.of(
              new FeedbackData.Improvement(
                  text, "Review the sentence for clarity and natural phrasing.", text)),
          List.of(),
          "Nice work—keep practicing and adding details to your answers.");
    }
  }

  static class OpenAiClient implements ConversationAiClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private final ObjectMapper mapper;
    private final RestClient http;
    private final String model;

    OpenAiClient(ObjectMapper m, RestClient h, String model) {
      mapper = m;
      http = h;
      this.model = model;
    }

    public String greeting() {
      return chat(
          List.of(
              Map.of("role", "system", "content", Prompts.CONVERSATION),
              Map.of(
                  "role",
                  "user",
                  "content",
                  "Start the conversation with one friendly greeting and question.")));
    }

    public String reply(List<ConversationMessage> messages) {
      var list = new ArrayList<Map<String, String>>();
      list.add(Map.of("role", "system", "content", Prompts.CONVERSATION));
      messages.stream()
          .skip(Math.max(0, messages.size() - 20))
          .forEach(
              x ->
                  list.add(
                      Map.of(
                          "role",
                          x.getRole() == MessageRole.USER ? "user" : "assistant",
                          "content",
                          x.getContent())));
      return chat(list);
    }

    public String translate(String englishText) {
      return chat(
          List.of(
              Map.of("role", "system", "content", Prompts.TRANSLATION),
              Map.of("role", "user", "content", englishText)));
    }

    public FeedbackData feedback(List<ConversationMessage> messages) {
      var transcript =
          messages.stream()
              .map(x -> x.getRole() + ": " + x.getContent())
              .reduce("", (a, b) -> a + b + "\n");
      String raw =
          chat(
              List.of(
                  Map.of("role", "system", "content", Prompts.FEEDBACK),
                  Map.of("role", "user", "content", transcript)));
      try {
        return mapper.readValue(
            raw.replaceAll("(?s)^```(?:json)?\\s*|\\s*```$", ""), FeedbackData.class);
      } catch (Exception e) {
        throw new IllegalStateException("Invalid feedback JSON", e);
      }
    }

    private String chat(List<Map<String, String>> messages) {
      long startedAt = System.nanoTime();
      try {
        JsonNode result =
            http.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", model, "messages", messages, "temperature", 0.5))
                .retrieve()
                .body(JsonNode.class);
        log.info(
            "action=callAi status=COMPLETED messageCount={} durationMs={}",
            messages.size(),
            Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
        return result.path("choices").path(0).path("message").path("content").asText();
      } catch (Exception exception) {
        log.error(
            "action=callAi status=FAILED messageCount={} durationMs={}",
            messages.size(),
            Duration.ofNanos(System.nanoTime() - startedAt).toMillis(),
            exception);
        throw exception;
      }
    }
  }
}
