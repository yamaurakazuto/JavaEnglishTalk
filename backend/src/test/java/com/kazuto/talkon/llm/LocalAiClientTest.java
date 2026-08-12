// ローカルAIが直近の話題へ反応し、複数種類の学習指摘を返すことを検証します。

package com.kazuto.talkon.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.kazuto.talkon.conversation.ConversationMessage;
import com.kazuto.talkon.conversation.ConversationSession;
import com.kazuto.talkon.conversation.MessageRole;
import com.kazuto.talkon.feedback.FeedbackCategory;
import com.kazuto.talkon.user.EnglishLevel;
import com.kazuto.talkon.user.User;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalAiClientTest {
  private final AiClientConfig.LocalAiClient ai = new AiClientConfig.LocalAiClient();
  private final ConversationSession session =
      new ConversationSession(new User("Learner", "learner@example.com", "hash"));

  @Test
  void replyBuildsOnTheLatestTopic() {
    var messages =
        List.of(
            message(MessageRole.ASSISTANT, "How was your weekend?", 1),
            message(MessageRole.USER, "I went hiking in the mountains.", 2));

    var response = ai.reply(messages, EnglishLevel.INTERMEDIATE);

    assertThat(response.text())
        .containsIgnoringCase("trail")
        .doesNotContain("How did that make you feel?");
    assertThat(response.inputTokens()).isZero();
    assertThat(response.outputTokens()).isZero();
  }

  @Test
  void feedbackHandlesDifferentCommonMistakesAndUsesConversationContent() {
    var messages =
        List.of(
            message(MessageRole.USER, "I am agree with my friend.", 1),
            message(MessageRole.USER, "I very like this restaurant.", 2));

    var feedback = ai.feedback(messages);

    assertThat(feedback.summary()).contains("I very like this restaurant.").contains("2回");
    assertThat(feedback.corrections())
        .extracting(correction -> correction.category())
        .containsExactly(FeedbackCategory.GRAMMAR, FeedbackCategory.NATURALNESS);
    assertThat(feedback.corrections())
        .extracting(correction -> correction.corrected())
        .containsExactly("I agree with my friend.", "I really like this restaurant.");
  }

  private ConversationMessage message(MessageRole role, String content, int sequenceNo) {
    return new ConversationMessage(session, role, content, sequenceNo);
  }
}
