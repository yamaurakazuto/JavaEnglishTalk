/**
 * 登録からフィードバック表示までをブラウザで検証します。利用者の主要正常系を通しで保証するE2Eテストです。
 */

import { test, expect } from "@playwright/test";
test("登録からフィードバック確認まで", async ({ page }) => {
  const email = `e2e-${Date.now()}@example.com`;
  await page.goto("/register");
  await page.getByLabel("表示名").fill("E2E User");
  await page.getByLabel("メールアドレス").fill(email);
  await page.getByLabel("パスワード").fill("password123");
  await page.getByRole("button", { name: "登録する" }).click();
  await page.getByLabel("メールアドレス").fill(email);
  await page.getByLabel("パスワード").fill("password123");
  await page.getByRole("button", { name: "ログイン" }).click();
  await page.getByRole("button", { name: "新しい会話を始める" }).click();
  await page.getByLabel("メッセージ").fill("I went hiking this weekend.");
  await page.getByRole("button", { name: "送信" }).click();
  await expect(page.getByText("That sounds interesting!")).toBeVisible();
  await page.getByRole("button", { name: "会話を終了" }).click();
  await expect(
    page.getByRole("heading", { name: "Conversation feedback" }),
  ).toBeVisible();
});
