/**
 * Playwrightの対象ディレクトリと開発サーバーを設定します。E2E実行条件を再現可能にするための設定です。
 */

import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./e2e",
  use: { baseURL: "http://localhost:5173" },
  webServer: {
    command: "npm run dev -- --host 127.0.0.1",
    url: "http://localhost:5173",
    reuseExistingServer: true,
  },
});
