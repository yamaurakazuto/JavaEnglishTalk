/**
 * 翻訳操作とフィードバック状態表示を検証します。会話支援機能の主要な回帰を防ぐためのテストです。
 */

import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { api, Conversation, Message } from "../../shared/api";
import { FeedbackPanel } from "./FeedbackPanel";
import { MessageList } from "./MessageList";

const assistantMessage: Message = {
  id: 10,
  role: "ASSISTANT",
  content: "How was your day?",
  translation: null,
  sequenceNo: 1,
  createdAt: "2026-08-07T00:00:00Z",
};

const conversation: Conversation = {
  id: 1,
  status: "ENDED",
  startedAt: "2026-08-07T00:00:00Z",
  finishedAt: "2026-08-07T00:05:00Z",
  messages: [assistantMessage],
  feedback: {
    status: "GENERATING",
    summary: null,
    strengths: [],
    improvements: [],
    corrections: [],
    overallComment: null,
    errorMessage: null,
  },
};

afterEach(() => vi.restoreAllMocks());

test("必要なときだけAIメッセージを翻訳する", async () => {
  const translated = {
    ...assistantMessage,
    translation: "今日はどうでしたか？",
  };
  vi.spyOn(api, "translate").mockResolvedValue(translated);
  const onMessageUpdated = vi.fn();

  render(
    <MessageList
      conversation={conversation}
      onMessageUpdated={onMessageUpdated}
      onError={vi.fn()}
    />,
  );
  await userEvent.click(screen.getByRole("button", { name: "日本語訳を見る" }));

  expect(api.translate).toHaveBeenCalledWith(1, 10);
  expect(onMessageUpdated).toHaveBeenCalledWith(translated);
});

test("フィードバック生成中と失敗を白画面にせず表示する", () => {
  const { rerender } = render(
    <FeedbackPanel
      conversation={conversation}
      retrying={false}
      onRetry={vi.fn()}
    />,
  );
  expect(
    screen.getByText("フィードバックを生成しています"),
  ).toBeInTheDocument();

  rerender(
    <FeedbackPanel
      conversation={{
        ...conversation,
        feedback: {
          ...conversation.feedback!,
          status: "FAILED",
          errorMessage:
            "フィードバックの生成に失敗しました。もう一度試してください。",
        },
      }}
      retrying={false}
      onRetry={vi.fn()}
    />,
  );
  expect(
    screen.getByRole("button", { name: "もう一度試す" }),
  ).toBeInTheDocument();
});
