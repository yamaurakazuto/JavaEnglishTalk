/** 会話履歴一覧の読込状態と詳細画面への導線を管理します。 */

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, HistoryPage as HistoryData, User } from "../../shared/api";
import { ErrorBox } from "../../shared/ui/ErrorBox";
import { Shell } from "../../shared/ui/Shell";
import { StatusBadge } from "../../shared/ui/StatusBadge";

type HistoryPageProps = { user: User; onLogout: () => void };

export function HistoryPage({ user, onLogout }: HistoryPageProps) {
  const [data, setData] = useState<HistoryData>();
  const [error, setError] = useState("");

  useEffect(() => {
    api
      .history()
      .then(setData)
      .catch((requestError: Error) => setError(requestError.message));
  }, []);

  return (
    <Shell user={user} onLogout={onLogout}>
      <p className="eyebrow">YOUR PROGRESS</p>
      <h1>会話履歴</h1>
      <ErrorBox error={error} />
      <div className="history">
        {data?.content.map((conversation) => (
          <Link key={conversation.id} to={`/history/${conversation.id}`}>
            <div>
              <strong>
                {new Date(conversation.startedAt).toLocaleString()}
              </strong>
              <StatusBadge status={conversation.status} />
            </div>
            <span>詳細を見る →</span>
          </Link>
        ))}
      </div>
      {data?.content.length === 0 && (
        <p className="history-empty">
          まだ会話履歴がありません。
          <br />
          ダッシュボードから最初の会話を始めてみましょう。
        </p>
      )}
    </Shell>
  );
}
