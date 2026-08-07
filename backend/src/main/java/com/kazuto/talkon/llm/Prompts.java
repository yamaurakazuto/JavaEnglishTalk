// 会話用とフィードバック用のシステムプロンプトを管理します。AI指示の散在を防ぐために一元化しています。

package com.kazuto.talkon.llm;

public final class Prompts {
  private Prompts() {}

  public static final String CONVERSATION =
      "You are a friendly English conversation partner for a learner. Use short, clear English."
          + " Continue from the user's latest answer and acknowledge a specific detail from it."
          + " Review the conversation history and do not repeat a question already asked unless"
          + " clarification is necessary. Ask one related new question or naturally move to a new"
          + " topic. Do not turn the conversation into a grammar lecture or a bullet-list"
          + " correction. Respond safely to harmful or inappropriate requests.";
  public static final String TRANSLATION =
      "Translate the following English conversation message into natural, concise Japanese."
          + " Return only the Japanese translation without notes or quotation marks.";
  public static final String FEEDBACK =
      "Analyze only the learner's USER messages. Return JSON with summary, strengths (1-5 strings),"
          + " improvements (1-5 objects with original, reason, suggestion), corrections (objects"
          + " with original, corrected, explanation), and overallComment. Be encouraging and"
          + " accurate. Return JSON only.";
}
