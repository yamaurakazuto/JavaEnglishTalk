// FakeまたはOpenAI互換LLMクライアントを構成します。環境に応じた実装切替を会話機能から隠蔽する設定です。

package com.kazuto.talkon.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazuto.talkon.conversation.ConversationMessage;
import com.kazuto.talkon.conversation.MessageRole;
import com.kazuto.talkon.feedback.FeedbackCategory;
import com.kazuto.talkon.feedback.FeedbackData;
import com.kazuto.talkon.user.EnglishLevel;
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
    public String greeting(EnglishLevel level) {
      return switch (level) {
        case BEGINNER -> "Hi! It's nice to meet you. How are you today?";
        case INTERMEDIATE -> "Hey! Nice to meet you. How's your day going?";
        case ADVANCED -> "Hey, great to meet you! What's been the highlight of your day so far?";
      };
    }

    public String reply(List<ConversationMessage> m, EnglishLevel level) {
      long turn = m.stream().filter(x -> x.getRole() == MessageRole.USER).count();
      var questions =
          List.of(
              "How did that make you feel?",
              "What happened next?",
              "Would you like to do that again?");
      if (turn % 3 == 0) {
        return level == EnglishLevel.BEGINNER
            ? "Oh, I understand. That sounds nice!"
            : "I get what you mean. That sounds like quite a day!";
      }
      String reaction =
          level == EnglishLevel.ADVANCED
              ? "I can relate to that—it sounds memorable. "
              : "Oh, I see! That sounds interesting. ";
      return reaction + questions.get((int) ((turn - 1) % questions.size()));
    }

    public String translate(String englishText) {
      if (englishText.contains("How are you today")) {
        return "今日は元気ですか？";
      }
      if (englishText.contains("How's your day going")) {
        return "今日はどんな一日を過ごしていますか？";
      }
      if (englishText.contains("highlight of your day")) {
        return "今日いちばん印象に残ったことは何ですか？";
      }
      if (englishText.contains("How did that make you feel")) {
        return "それについて、どんな気持ちになりましたか？";
      }
      if (englishText.contains("What happened next")) {
        return "そのあと、どうなりましたか？";
      }
      if (englishText.contains("Would you like to do that again")) {
        return "またやってみたいですか？";
      }
      return "なるほど、よく分かります。それは印象に残る出来事ですね。";
    }

    public FeedbackData feedback(List<ConversationMessage> m) {
      var text =
          m.stream()
              .filter(x -> x.getRole() == MessageRole.USER)
              .findFirst()
              .map(ConversationMessage::getContent)
              .orElse("Your message");
      var corrections =
          text.equalsIgnoreCase("Today is tired.")
              ? List.of(
                  new FeedbackData.Correction(
                      text,
                      "I'm tired today.",
                      "tired は人の状態を表すため、I を主語にします。",
                      "I feel tired today.",
                      FeedbackCategory.GRAMMAR))
              : List.<FeedbackData.Correction>of();
      return new FeedbackData(
          "日常の話題について英語で会話を続けられました。",
          List.of("自分の考えを英語で伝え、会話を前へ進められました。"),
          corrections,
          List.of("会話では短い文でも、具体的な情報を一つ加えると自然に広がります。"),
          "よくできました。間違いを恐れず、これからも会話を楽しみましょう。");
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

    public String greeting(EnglishLevel level) {
      return chat(
          List.of(
              Map.of(
                  "role",
                  "system",
                  "content",
                  Prompts.CONVERSATION + "\nLEVEL POLICY:\n" + Prompts.levelPolicy(level)),
              Map.of(
                  "role",
                  "user",
                  "content",
                  "Start the conversation with one friendly greeting and question.")));
    }

    public String reply(List<ConversationMessage> messages, EnglishLevel level) {
      var list = new ArrayList<Map<String, String>>();
      list.add(
          Map.of(
              "role",
              "system",
              "content",
              Prompts.CONVERSATION + "\nLEVEL POLICY:\n" + Prompts.levelPolicy(level)));
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
