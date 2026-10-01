/**
 * 学習状況と会話・履歴への入口をまとめます。迷わず次の操作を選べるシンプルなダッシュボード画面です。
 */

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, DashboardData } from "../../shared/api";
import { ActivityGrid } from "./ActivityGrid";
import { StudySummary } from "./StudySummary";

type DashboardPageProps = {
  displayName: string;
};

export function DashboardPage({ displayName }: DashboardPageProps) {
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState<DashboardData>();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    api
      .dashboard()
      .then(setDashboard)
      .catch((requestError: Error) => setError(requestError.message));
  }, []);

  async function openConversation() {
    setBusy(true);
    setError("");
    try {
      const conversation = await api.start();
      navigate(`/conversations/${conversation.id}`);
    } catch (requestError) {
      setError((requestError as Error).message);
    } finally {
      setBusy(false);
    }
  }

  if (!dashboard) {
    return (
      <p className="dashboard-loading">
        {error || "学習記録を読み込んでいます…"}
      </p>
    );
  }

  return (
    <div className="dashboard">
      <section className="dashboard-intro">
        <div>
          <p className="eyebrow">YOUR ENGLISH PRACTICE</p>
          <h1>こんにちは、{displayName}さん</h1>
          <p>今日も少しだけ、英語で話してみましょう。</p>
        </div>
        <button
          className="start-button"
          onClick={openConversation}
          disabled={busy}
        >
          <span>＋</span>
          {busy
            ? "準備中…"
            : dashboard.activeConversationId
              ? "会話を再開する"
              : "会話を始める"}
        </button>
      </section>

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}

      <StudySummary
        todayStudySeconds={dashboard.todayStudySeconds}
        currentStreakDays={dashboard.currentStreakDays}
        totalStudyDays={dashboard.totalStudyDays}
      />

      <ActivityGrid activities={dashboard.activities} />

      <section className="dashboard-actions">
        <button
          className="action-card conversation-action"
          onClick={openConversation}
          disabled={busy}
        >
          <span className="action-icon">→</span>
          <span>
            <strong>
              {dashboard.activeConversationId ? "会話を続ける" : "新しい会話"}
            </strong>
            <small>AIと自由に英語で話す</small>
          </span>
        </button>

        <button
          className="action-card history-action"
          onClick={() => navigate("/history")}
        >
          <span className="action-icon">↺</span>
          <span>
            <strong>過去の会話を振り返る</strong>
            <small>会話内容とフィードバックを見る</small>
          </span>
        </button>
      </section>
    </div>
  );
}
