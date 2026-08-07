/**
 * ViteとVitestの実行設定を定義します。開発サーバーとテスト環境を共通管理するための設定です。
 */

import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
export default defineConfig({
  plugins: [react()],
  server: { port: 5173 },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: "./src/test/setup.ts",
    exclude: ["e2e/**", "node_modules/**"],
  },
});
