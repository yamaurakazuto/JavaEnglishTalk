/**
 * 初回レベル選択と保存操作を検証します。オンボーディングを通らず会話が始まる回帰を防ぐテストです。
 */

import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { api } from "../../shared/api";
import { EnglishLevelPage } from "./EnglishLevelPage";

afterEach(() => vi.restoreAllMocks());

test("選択した英会話レベルをUser Profileへ保存する", async () => {
  const savedUser = {
    id: 1,
    displayName: "Learner",
    email: "learner@example.com",
    englishLevel: "BEGINNER" as const,
  };
  vi.spyOn(api, "selectEnglishLevel").mockResolvedValue(savedUser);
  const onSelected = vi.fn();

  render(<EnglishLevelPage onSelected={onSelected} />);
  await userEvent.click(screen.getByRole("button", { name: /初心者/ }));

  expect(api.selectEnglishLevel).toHaveBeenCalledWith("BEGINNER");
  expect(onSelected).toHaveBeenCalledWith(savedUser);
});
