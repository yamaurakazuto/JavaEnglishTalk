/**
 * 今日の学習時間と連続学習日数を表示します。最初に確認したい数字をまとめるためのコンポーネントです。
 */

type StudySummaryProps = {
  todayStudySeconds: number;
  currentStreakDays: number;
  totalStudyDays: number;
};

export function StudySummary({
  todayStudySeconds,
  currentStreakDays,
  totalStudyDays,
}: StudySummaryProps) {
  return (
    <section className="study-summary">
      <article className="dashboard-card primary-stat">
        <span className="stat-icon">◷</span>
        <div>
          <p>今日の学習時間</p>
          <strong>{formatStudyTime(todayStudySeconds)}</strong>
        </div>
      </article>

      <article className="dashboard-card stat-card">
        <span className="stat-icon warm">●</span>
        <div>
          <p>連続学習</p>
          <strong>{currentStreakDays}日</strong>
        </div>
      </article>

      <article className="dashboard-card stat-card">
        <span className="stat-icon soft">✓</span>
        <div>
          <p>学習した日</p>
          <strong>{totalStudyDays}日</strong>
        </div>
      </article>
    </section>
  );
}

function formatStudyTime(seconds: number) {
  if (seconds < 60) {
    return seconds > 0 ? "1分未満" : "0分";
  }

  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (hours === 0) {
    return `${minutes}分`;
  }
  return `${hours}時間 ${minutes}分`;
}
