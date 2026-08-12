// FakeまたはOpenAI互換LLMクライアントを構成します。環境に応じた実装切替を会話機能から隠蔽する設定です。

package com.kazuto.talkon.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazuto.talkon.conversation.ConversationAIService.AiResponse;
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
      @Value("${app.llm.timeout-seconds:30}") int timeout,
      @Value("${app.llm.history-limit:20}") int historyLimit) {
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
            model,
            historyLimit);
  }

  static class LocalAiClient implements ConversationAiClient {
    public AiResponse greeting(EnglishLevel level) {
      String text =
          switch (level) {
            case BEGINNER -> "Hi! It's nice to meet you. How are you today?";
            case INTERMEDIATE -> "Hey! Nice to meet you. How's your day going?";
            case ADVANCED ->
                "Hey, great to meet you! What's been the highlight of your day so far?";
          };
      return new AiResponse(text, 0, 0, "local-llm");
    }

    public AiResponse reply(List<ConversationMessage> m, EnglishLevel level) {
      long turn = m.stream().filter(x -> x.getRole() == MessageRole.USER).count();
      String latest =
          m.stream()
              .filter(x -> x.getRole() == MessageRole.USER)
              .reduce((first, second) -> second)
              .map(ConversationMessage::getContent)
              .orElse("")
              .toLowerCase();
      if (latest.contains("tired") || latest.contains("busy")) {
        return localResponse(
            level == EnglishLevel.BEGINNER
                ? "That sounds tiring. I hope you can relax soon. What helps you rest?"
                : "That sounds exhausting. I hope you get a chance to unwind—what usually helps you recharge?");
      }
      if (latest.contains("hiking") || latest.contains("mountain") || latest.contains("walk")) {
        return localResponse(
            level == EnglishLevel.BEGINNER
                ? "That sounds fun! I like being outside too. Where do you usually go?"
                : "That sounds like a great way to spend the day. Do you have a favorite trail or place to walk?");
      }
      if (latest.contains("food") || latest.contains("cook") || latest.contains("restaurant")) {
        return localResponse(
            level == EnglishLevel.BEGINNER
                ? "Nice! Food is always fun to talk about. What dish do you like best?"
                : "Now you're making me hungry! What's a dish you could happily eat again and again?");
      }
      if (turn % 3 == 0) {
        return localResponse(
            level == EnglishLevel.BEGINNER
                ? "I see. Thanks for telling me!"
                : "I get what you mean. Thanks for sharing that with me.");
      }
      return localResponse(
          level == EnglishLevel.ADVANCED
              ? "That's an interesting point. What stands out to you most about it?"
              : "Oh, I see! What do you like most about it?");
    }

    private static AiResponse localResponse(String text) {
      return new AiResponse(text, 0, 0, "local-llm");
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
      var userMessages =
          m.stream()
              .filter(x -> x.getRole() == MessageRole.USER)
              .map(ConversationMessage::getContent)
              .toList();
      var corrections = new ArrayList<FeedbackData.Correction>();
      userMessages.forEach(text -> addLocalCorrections(text, corrections));
      var topic = userMessages.isEmpty() ? "身近な話題" : "「" + userMessages.getLast() + "」という話題";
      return new FeedbackData(
          topic + "について、" + userMessages.size() + "回の発言で会話を進めました。",
          List.of("質問に答えるだけでなく、自分の情報を英語で伝えられました。"),
          corrections.stream().limit(5).toList(),
          List.of("理由や具体例を一つ加えると、相手が次の話題を広げやすくなります。"),
          corrections.isEmpty()
              ? "意味は自然に伝わっています。次は具体例を添えて、会話をもう一段広げてみましょう。"
              : "伝えたい内容は理解できます。今回の修正を一つ選び、次の会話で使ってみましょう。");
    }

    private static void addLocalCorrections(
        String text, List<FeedbackData.Correction> corrections) {
      String normalized = text.trim().toLowerCase();
      if (normalized.contains("today is tired")) {
        corrections.add(
            new FeedbackData.Correction(
                text,
                "I'm tired today.",
                "tired は人の状態を表すため、I を主語にします。",
                "I feel tired today.",
                FeedbackCategory.GRAMMAR));
      } else if (normalized.contains("i am agree")) {
        corrections.add(
            new FeedbackData.Correction(
                text,
                text.replaceAll("(?i)I am agree", "I agree"),
                "agree は動詞なので、be動詞の am は付けません。",
                "I feel the same way.",
                FeedbackCategory.GRAMMAR));
      } else if (normalized.contains("i very like")) {
        corrections.add(
            new FeedbackData.Correction(
                text,
                text.replaceAll("(?i)I very like", "I really like"),
                "動詞 like を強めるときは very ではなく really を使うと自然です。",
                "I'm a big fan of it.",
                FeedbackCategory.NATURALNESS));
      }
    }
  }

  static class OpenAiClient implements ConversationAiClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private final ObjectMapper mapper;
    private final RestClient http;
    private final String model;
    private final int historyLimit;

    OpenAiClient(ObjectMapper m, RestClient h, String model, int historyLimit) {
      mapper = m;
      http = h;
      this.model = model;
      this.historyLimit = historyLimit;
    }

    public AiResponse greeting(EnglishLevel level) {
      return chatWithUsage(
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

    public AiResponse reply(List<ConversationMessage> messages, EnglishLevel level) {
      var list = new ArrayList<Map<String, String>>();
      list.add(
          Map.of(
              "role",
              "system",
              "content",
              Prompts.CONVERSATION + "\nLEVEL POLICY:\n" + Prompts.levelPolicy(level)));
      messages.stream()
          .skip(Math.max(0, messages.size() - historyLimit))
          .forEach(
              x ->
                  list.add(
                      Map.of(
                          "role",
                          x.getRole() == MessageRole.USER ? "user" : "assistant",
                          "content",
                          x.getContent())));
      return chatWithUsage(list);
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
      return chatWithUsage(messages).text();
    }

    private AiResponse chatWithUsage(List<Map<String, String>> messages) {
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
            "action=callAi status=COMPLETED model={} messageCount={} inputTokens={} outputTokens={} durationMs={}",
            model,
            messages.size(),
            result.path("usage").path("prompt_tokens").asInt(-1),
            result.path("usage").path("completion_tokens").asInt(-1),
            Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
        return new AiResponse(
            result.path("choices").path(0).path("message").path("content").asText(),
            result.path("usage").path("prompt_tokens").asInt(0),
            result.path("usage").path("completion_tokens").asInt(0),
            model);
      } catch (Exception exception) {
        log.error(
            "action=callAi status=FAILED model={} messageCount={} durationMs={}",
            model,
            messages.size(),
            Duration.ofNanos(System.nanoTime() - startedAt).toMillis(),
            exception);
        throw exception;
      }
    }
  }
}
