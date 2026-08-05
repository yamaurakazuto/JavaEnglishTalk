/**
 * 認証ガードとフォーム制約を検証します。主要UI動作の回帰を防ぐためのコンポーネントテストです。
 */

import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import App from "./App";
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
