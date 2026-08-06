/**
 * GitHubの草に似た学習カレンダーを表示します。日ごとの学習量を色の濃さで直感的に見せるためのコンポーネントです。
 */

import { DailyActivity } from "../../shared/api";

type ActivityGridProps = {
  activities: DailyActivity[];
};

export function ActivityGrid({ activities }: ActivityGridProps) {
  return (
    <section className="dashboard-card activity-card">
      <div className="card-heading">
        <div>
          <p className="eyebrow">LEARNING ACTIVITY</p>
          <h2>学習の継続</h2>
        </div>
        <span className="activity-period">直近1年間</span>
      </div>

      <div className="activity-scroll">
        <div className="activity-grid" aria-label="日ごとの学習記録">
          {activities.map((activity) => (
            <span
              key={activity.date}
              className={`activity-cell level-${activity.level}`}
              aria-label={`${activity.date}: ${activity.sessionCount}回学習`}
              title={`${activity.date}・${activity.sessionCount}回・${formatDuration(activity.studySeconds)}`}
            />
          ))}
        </div>
      </div>

      <div className="activity-legend" aria-hidden="true">
        <span>少ない</span>
        {[0, 1, 2, 3, 4].map((level) => (
          <span key={level} className={`activity-cell level-${level}`} />
        ))}
        <span>多い</span>
      </div>
    </section>
  );
}

function formatDuration(seconds: number) {
  if (seconds < 60) {
    return seconds > 0 ? "1分未満" : "0分";
  }
  return `${Math.floor(seconds / 60)}分`;
}
