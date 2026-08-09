/**
 * 認証ガードとフォーム制約を検証します。主要UI動作の回帰を防ぐためのコンポーネントテストです。
 */

import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import App from "./App";
import { api, Conversation } from "./shared/api";
afterEach(() => vi.restoreAllMocks());
test("未認証ユーザーをログイン画面へ誘導する", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValue(
    new Response(JSON.stringify({ message: "login" }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    }),
  );
  render(
    <MemoryRouter initialEntries={["/"]}>
      <App />
    </MemoryRouter>,
  );
  expect(
    await screen.findByRole("heading", { name: "おかえりなさい" }),
  ).toBeInTheDocument();
});
test("ログインフォームには入力制約がある", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValue(
    new Response(JSON.stringify({ message: "login" }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    }),
  );
  render(
    <MemoryRouter initialEntries={["/login"]}>
      <App />
    </MemoryRouter>,
  );
  await waitFor(() =>
    expect(screen.getByLabelText("メールアドレス")).toHaveAttribute(
      "type",
      "email",
    ),
  );
  expect(screen.getByLabelText("パスワード")).toHaveAttribute("minlength", "8");
});

test("会話画面の上部と入力欄の下から会話を終了できる", async () => {
  const activeConversation: Conversation = {
    id: 1,
    status: "ACTIVE",
    startedAt: "2026-08-08T00:00:00Z",
    finishedAt: null,
    messages: [
      {
        id: 1,
        role: "ASSISTANT",
        content: "How are you?",
        translation: null,
        sequenceNo: 1,
        createdAt: "2026-08-08T00:00:00Z",
      },
    ],
    feedback: null,
  };
  vi.spyOn(api, "me").mockResolvedValue({
    id: 1,
    displayName: "Learner",
    email: "learner@example.com",
    englishLevel: "INTERMEDIATE",
  });
  vi.spyOn(api, "detail").mockResolvedValue(activeConversation);
  const finish = vi.spyOn(api, "finish").mockResolvedValue({
    ...activeConversation,
    status: "ENDED",
    finishedAt: "2026-08-08T00:05:00Z",
    feedback: {
      status: "GENERATING",
      summary: null,
      strengths: [],
      corrections: [],
      vocabularyTips: [],
      overallComment: null,
      errorMessage: null,
    },
  });

  render(
    <MemoryRouter initialEntries={["/conversations/1"]}>
      <App />
    </MemoryRouter>,
  );

  const finishButtons = await screen.findAllByRole("button", {
    name: "会話を終了",
  });
  expect(finishButtons).toHaveLength(2);
  await userEvent.click(finishButtons[1]);
  expect(finish).toHaveBeenCalledWith(1);
});
