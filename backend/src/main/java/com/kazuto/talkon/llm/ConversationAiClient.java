package com.kazuto.talkon.llm;
import com.kazuto.talkon.conversation.ConversationMessage;
import com.kazuto.talkon.feedback.FeedbackData;
import java.util.List;
public interface ConversationAiClient {
 String greeting();
 String reply(List<ConversationMessage> messages);
 FeedbackData feedback(List<ConversationMessage> messages);
}

