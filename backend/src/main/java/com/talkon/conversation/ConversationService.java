// 会話の開始・送信・終了・フィードバック生成を制御します。状態遷移とトランザクション境界を一箇所で管理するServiceです。

package com.talkon.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talkon.common.ApiException;
import com.talkon.feedback.ConversationFeedback;
import com.talkon.feedback.ConversationFeedbackRepository;
import com.talkon.feedback.FeedbackGenerationService;
import com.talkon.feedback.FeedbackStatus;
import com.talkon.user.EnglishLevel;
import com.talkon.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ConversationService {
  private static final Logger log = LoggerFactory.getLogger(ConversationService.class);
  private static final Pattern ENGLISH_WORD = Pattern.compile("[A-Za-z]+(?:['’][A-Za-z]+)*");

  private final ConversationSessionRepository sessions;
  private final ConversationMessageRepository messages;
  private final ConversationFeedbackRepository feedbacks;
  private final UserRepository users;
  private final ConversationAIService ai;
  private final TranslationService translations;
  private final LlmCostCalculator costs;
  private final ObjectMapper mapper;
  private final FeedbackGenerationService feedbackGenerator;
  private final TransactionTemplate tx;

  public ConversationService(
      ConversationSessionRepository s,
      ConversationMessageRepository m,
      ConversationFeedbackRepository f,
      UserRepository u,
      ConversationAIService a,
      TranslationService translations,
      LlmCostCalculator costs,
      ObjectMapper mapper,
      FeedbackGenerationService feedbackGenerator,
      TransactionTemplate tx) {
    sessions = s;
    messages = m;
    feedbacks = f;
    users = u;
    ai = a;
    this.translations = translations;
    this.costs = costs;
    this.mapper = mapper;
    this.feedbackGenerator = feedbackGenerator;
    this.tx = tx;
  }

  public StartResult start(Long userId) {
    var existing =
        sessions.findFirstByUserIdAndStatusOrderByStartedAtDesc(userId, ConversationStatus.ACTIVE);
    if (existing.isPresent()) {
      return new StartResult(detail(existing.get(), userId), false);
    }
    final ConversationAIService.AiResponse greeting;
    try {
      greeting = ai.greeting(level(userId));
    } catch (Exception e) {
      throw llm();
    }
    var created =
        tx.execute(
            status -> {
              var user = users.findLockedById(userId).orElseThrow();
              var active =
                  sessions.findFirstByUserIdAndStatusOrderByStartedAtDesc(
                      userId, ConversationStatus.ACTIVE);
              if (active.isPresent()) {
                return new Created(active.get(), false);
              }
              var s = new ConversationSession(user);
              recordUsage(s, greeting);
              sessions.save(s);
              messages.save(new ConversationMessage(s, MessageRole.ASSISTANT, greeting.text(), 1));
              return new Created(s, true);
            });
    return new StartResult(detail(created.session(), userId), created.created());
  }

  public ConversationDtos.Detail active(Long userId) {
    return sessions
        .findFirstByUserIdAndStatusOrderByStartedAtDesc(userId, ConversationStatus.ACTIVE)
        .map(s -> detail(s, userId))
        .orElse(null);
  }

  public ConversationDtos.Detail detail(Long id, Long userId) {
    return detail(owned(id, userId), userId);
  }

  private ConversationDtos.Detail detail(ConversationSession s, Long userId) {
    var ms =
        messages.findBySessionIdOrderBySequenceNo(s.getId()).stream()
            .map(ConversationDtos::message)
            .toList();
    var f =
        feedbacks
            .findBySessionId(s.getId())
            .map(x -> ConversationDtos.feedback(x, mapper))
            .orElse(null);
    return new ConversationDtos.Detail(
        s.getId(),
        s.getStatus().name(),
        s.getStartedAt(),
        s.getFinishedAt(),
        ms,
        f,
        new ConversationDtos.LlmUsageResponse(
            s.getLlmInputTokens(), s.getLlmOutputTokens(), s.getLlmCostMicros(), s.getLlmModel()));
  }

  public ConversationDtos.Detail send(Long id, Long userId, String raw) {
    Instant startedAt = Instant.now();
    var content = raw == null ? "" : raw.trim();
    if (content.isEmpty() || content.length() > 2000) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "メッセージは1〜2,000文字で入力してください。");
    }
    tx.executeWithoutResult(
        x -> {
          var s = locked(id, userId);
          if (s.getStatus() != ConversationStatus.ACTIVE) {
            throw conflict("終了済みの会話には送信できません。");
          }
          if (messages.countBySessionIdAndRole(id, MessageRole.USER) >= 50) {
            throw conflict("1会話の上限に達しました。");
          }
          int n = messages.findBySessionIdOrderBySequenceNo(id).size() + 1;
          messages.save(new ConversationMessage(s, MessageRole.USER, content, n));
        });
    ConversationAIService.AiResponse reply;
    try {
      reply = ai.reply(messages.findBySessionIdOrderBySequenceNo(id), level(userId));
    } catch (Exception e) {
      throw llm();
    }
    tx.executeWithoutResult(
        x -> {
          var s = locked(id, userId);
          if (s.getStatus() != ConversationStatus.ACTIVE) {
            throw conflict("会話の状態が変更されました。");
          }
          int n = messages.findBySessionIdOrderBySequenceNo(id).size() + 1;
          messages.save(new ConversationMessage(s, MessageRole.ASSISTANT, reply.text(), n));
          recordUsage(s, reply);
        });
    log.info(
        "userId={} conversationId={} action=sendMessage status=COMPLETED durationMs={}",
        userId,
        id,
        Duration.between(startedAt, Instant.now()).toMillis());
    return detail(id, userId);
  }

  public ConversationDtos.Detail finish(Long id, Long userId) {
    Instant startedAt = Instant.now();
    tx.executeWithoutResult(
        x -> {
          var s = locked(id, userId);
          if (s.getStatus() != ConversationStatus.ACTIVE) {
            throw conflict("会話を終了できる状態ではありません。");
          }
          if (messages.countBySessionIdAndRole(id, MessageRole.USER) == 0) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "1件以上メッセージを送信してください。");
          }
          s.end();
          feedbacks.save(new ConversationFeedback(s));
          runAfterCommit(() -> feedbackGenerator.generate(id, userId));
        });
    log.info(
        "userId={} conversationId={} action=finishConversation status=ENDED durationMs={}",
        userId,
        id,
        Duration.between(startedAt, Instant.now()).toMillis());
    return detail(id, userId);
  }

  public ConversationDtos.Detail retryFeedback(Long id, Long userId) {
    tx.executeWithoutResult(
        ignored -> {
          owned(id, userId);
          var feedback =
              feedbacks.findBySessionId(id).orElseThrow(() -> conflict("フィードバックが見つかりません。"));
          if (feedback.getStatus() != FeedbackStatus.FAILED) {
            throw conflict("再生成できる状態ではありません。");
          }
          feedback.retry();
          runAfterCommit(() -> feedbackGenerator.generate(id, userId));
        });
    return detail(id, userId);
  }

  public ConversationDtos.MessageResponse translate(
      Long conversationId, Long messageId, Long userId) {
    owned(conversationId, userId);
    var message =
        messages
            .findByIdAndSessionId(messageId, conversationId)
            .orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "メッセージが見つかりません。"));
    if (message.getRole() != MessageRole.ASSISTANT) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "AIメッセージだけ翻訳できます。");
    }
    if (message.getTranslation() == null) {
      String translated;
      try {
        translated = translations.translate(message.getContent());
      } catch (Exception exception) {
        throw llm();
      }
      String finalTranslated = translated;
      tx.executeWithoutResult(
          ignored ->
              messages
                  .findByIdAndSessionId(messageId, conversationId)
                  .orElseThrow()
                  .translate(finalTranslated));
    }
    return ConversationDtos.message(
        messages.findByIdAndSessionId(messageId, conversationId).orElseThrow());
  }

  public ConversationDtos.WordTranslationResponse translateWord(
      Long conversationId, Long messageId, Long userId, String requestedWord) {
    owned(conversationId, userId);
    var message =
        messages
            .findByIdAndSessionId(messageId, conversationId)
            .filter(item -> item.getRole() == MessageRole.ASSISTANT)
            .orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "AIメッセージが見つかりません。"));
    var word = requestedWord.trim();
    if (!ENGLISH_WORD.matcher(word).matches() || !containsWord(message.getContent(), word)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_WORD", "翻訳する英単語を確認してください。");
    }
    try {
      var translation = translations.translateWord(word, message.getContent()).trim();
      if (translation.isEmpty()) {
        throw new IllegalStateException("Word translation was empty");
      }
      return new ConversationDtos.WordTranslationResponse(word, translation);
    } catch (ApiException exception) {
      throw exception;
    } catch (Exception exception) {
      throw llm();
    }
  }

  public ConversationDtos.PageResponse history(Long userId, int page, int size) {
    var safePage = Math.max(0, page);
    var safeSize = Math.max(1, Math.min(50, size));
    var p = sessions.findByUserIdOrderByStartedAtDesc(userId, PageRequest.of(safePage, safeSize));
    var content =
        p.getContent().stream()
            .map(
                s ->
                    new ConversationDtos.Summary(
                        s.getId(), s.getStatus().name(), s.getStartedAt(), s.getFinishedAt()))
            .toList();
    return new ConversationDtos.PageResponse(
        content, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
  }

  private ConversationSession owned(Long id, Long userId) {
    return sessions
        .findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "会話が見つかりません。"));
  }

  private ConversationSession locked(Long id, Long userId) {
    return sessions
        .lockOwned(id, userId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "会話が見つかりません。"));
  }

  private static ApiException conflict(String m) {
    return new ApiException(HttpStatus.CONFLICT, "CONFLICT", m);
  }

  private static boolean containsWord(String sentence, String requestedWord) {
    var matcher = ENGLISH_WORD.matcher(sentence);
    while (matcher.find()) {
      if (matcher.group().equalsIgnoreCase(requestedWord)) {
        return true;
      }
    }
    return false;
  }

  private static ApiException llm() {
    return new ApiException(
        HttpStatus.SERVICE_UNAVAILABLE, "LLM_UNAVAILABLE", "AIサービスを利用できません。しばらくしてから再試行してください。");
  }

  private EnglishLevel level(Long userId) {
    var level = users.findById(userId).orElseThrow().getEnglishLevel();
    if (level == null) {
      throw new ApiException(HttpStatus.CONFLICT, "ENGLISH_LEVEL_REQUIRED", "英会話レベルを選択してください。");
    }
    return level;
  }

  private void recordUsage(ConversationSession session, ConversationAIService.AiResponse response) {
    session.addLlmUsage(
        response.inputTokens(),
        response.outputTokens(),
        costs.estimateMicros(response.inputTokens(), response.outputTokens()),
        response.model());
  }

  private void runAfterCommit(Runnable action) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            action.run();
          }
        });
  }

  public record StartResult(ConversationDtos.Detail detail, boolean created) {}

  private record Created(ConversationSession session, boolean created) {}
}
