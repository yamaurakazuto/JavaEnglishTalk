/**
 * ダッシュボードの学習情報と主要導線を検証します。シンプルな画面要件を変更時にも守るためのテストです。
 */

import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, test, vi } from "vitest";
import { api, DashboardData } from "../../shared/api";
import { DashboardPage } from "./DashboardPage";

afterEach(() => vi.restoreAllMocks());

test("学習時間、継続情報、会話と履歴の導線を表示する", async () => {
  const dashboard: DashboardData = {
    todayStudySeconds: 1_800,
    currentStreakDays: 3,
    totalStudyDays: 12,
    activeConversationId: null,
    activities: [
      {
        date: "2026-08-06",
        sessionCount: 1,
        studySeconds: 1_800,
        level: 1,
      },
    ],
  };
  vi.spyOn(api, "dashboard").mockResolvedValue(dashboard);

  render(
    <MemoryRouter>
      <DashboardPage displayName="Test User" />
    </MemoryRouter>,
  );

  expect(await screen.findByText("30分")).toBeInTheDocument();
  expect(screen.getByText("3日")).toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: /新しい会話/ }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: /過去の会話を振り返る/ }),
  ).toBeInTheDocument();
});
