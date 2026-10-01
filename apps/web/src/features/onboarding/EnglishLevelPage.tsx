/**
 * 初回利用者に英会話レベルを選んでもらいます。毎回質問せずUser Profileへ一度保存するオンボーディング画面です。
 */

import { useState } from "react";
import { api, EnglishLevel, User } from "../../shared/api";

type EnglishLevelPageProps = {
  onSelected: (user: User) => void;
};

const levels: {
  value: EnglishLevel;
  icon: string;
  title: string;
  text: string;
}[] = [
  {
    value: "BEGINNER",
    icon: "🌱",
    title: "初心者",
    text: "簡単な英語から練習したい",
  },
  {
    value: "INTERMEDIATE",
    icon: "🌿",
    title: "中級",
    text: "日常会話なら少しできる",
  },
  {
    value: "ADVANCED",
    icon: "🌳",
    title: "上級",
    text: "より自然な英会話を練習したい",
  },
];

export function EnglishLevelPage({ onSelected }: EnglishLevelPageProps) {
  const [busy, setBusy] = useState<EnglishLevel>();
  const [error, setError] = useState("");

  async function select(level: EnglishLevel) {
    setBusy(level);
    setError("");
    try {
      onSelected(await api.selectEnglishLevel(level));
    } catch (requestError) {
      setError((requestError as Error).message);
      setBusy(undefined);
    }
  }

  return (
    <main className="onboarding">
      <section>
        <p className="eyebrow">WELCOME TO TALKON</p>
        <h1>どんな英語で話したいですか？</h1>
        <p className="muted">
          選んだレベルに合わせて、会話の難しさを調整します。
        </p>
        {error && <p className="error">{error}</p>}
        <div className="level-options">
          {levels.map((level) => (
            <button
              key={level.value}
              className="level-card"
              disabled={Boolean(busy)}
              onClick={() => select(level.value)}
            >
              <span>{level.icon}</span>
              <strong>{level.title}</strong>
              <small>{level.text}</small>
            </button>
          ))}
        </div>
      </section>
    </main>
  );
}
