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
      return "That sounds interesting! Could you tell me a little more about it?";
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
      messages.forEach(
          x ->
              list.add(
                  Map.of(
                      "role",
                      x.getRole() == MessageRole.USER ? "user" : "assistant",
                      "content",
                      x.getContent())));
      return chat(list);
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
      JsonNode result =
          http.post()
              .uri("/chat/completions")
              .contentType(MediaType.APPLICATION_JSON)
              .body(Map.of("model", model, "messages", messages, "temperature", 0.5))
              .retrieve()
              .body(JsonNode.class);
      return result.path("choices").path(0).path("message").path("content").asText();
    }
  }
}
