/**
 * 翻訳操作とフィードバック状態表示を検証します。会話支援機能の主要な回帰を防ぐためのテストです。
 */

import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
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
    corrections: [],
    vocabularyTips: [],
    overallComment: null,
    errorMessage: null,
  },
  llmUsage: {
    inputTokens: 1_200,
    outputTokens: 80,
    estimatedCostMicros: 97_280,
    model: "gpt-4.1-mini",
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
  await userEvent.click(screen.getByRole("button", { name: /日本語訳を見る/ }));

  expect(api.translate).toHaveBeenCalledWith(1, 10);
  expect(onMessageUpdated).toHaveBeenCalledWith(translated);
});

test("教材形式で修正文、日本語理由、別表現を表示する", () => {
  render(
    <MemoryRouter>
      <FeedbackPanel
        conversation={{
          ...conversation,
          feedback: {
            status: "COMPLETED",
            summary: "会話できました。",
            strengths: ["気持ちを伝えられました。"],
            corrections: [
              {
                original: "Today is tired.",
                corrected: "I'm tired today.",
                reasonJa: "tired は人の状態を表します。",
                alternative: "I feel tired today.",
                category: "GRAMMAR",
              },
            ],
            vocabularyTips: ["tired は疲れたという意味です。"],
            overallComment: "よくできました。",
            errorMessage: null,
          },
        }}
        retrying={false}
        onRetry={vi.fn()}
      />
    </MemoryRouter>,
  );

  expect(screen.getByText("✕ Today is tired.")).toBeInTheDocument();
  expect(screen.getByText("✓ I'm tired today.")).toBeInTheDocument();
  expect(screen.getByText("tired は人の状態を表します。")).toBeInTheDocument();
  expect(screen.getByText("I feel tired today.")).toBeInTheDocument();
  expect(screen.getByText("1,280 tokens")).toBeInTheDocument();
  expect(screen.getByText("約 0.0973 円")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "ホームへ戻る" })).toHaveAttribute(
    "href",
    "/",
  );
});

test("フィードバック生成中と失敗を白画面にせず表示する", () => {
  const { rerender } = render(
    <MemoryRouter>
      <FeedbackPanel
        conversation={conversation}
        retrying={false}
        onRetry={vi.fn()}
      />
    </MemoryRouter>,
  );
  expect(
    screen.getByText("フィードバックを生成しています"),
  ).toBeInTheDocument();

  rerender(
    <MemoryRouter>
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
      />
    </MemoryRouter>,
  );
  expect(
    screen.getByRole("button", { name: "もう一度試す" }),
  ).toBeInTheDocument();
});
