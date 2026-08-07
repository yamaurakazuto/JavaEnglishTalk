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
              tabIndex={0}
              aria-label={`${formatDate(activity.date)}、${activity.sessionCount} conversations、${activity.messageCount} messages`}
              data-tooltip={`${formatDate(activity.date)}\n${activity.sessionCount} conversations\n${activity.messageCount} messages`}
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

function formatDate(date: string) {
  return new Intl.DateTimeFormat("ja-JP", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date(`${date}T00:00:00`));
}
