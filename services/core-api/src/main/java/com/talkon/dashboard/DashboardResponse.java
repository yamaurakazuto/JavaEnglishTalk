// ダッシュボードに必要な学習時間と継続情報をまとめます。画面が一度の通信で表示できるようにするResponse DTOです。

package com.talkon.dashboard;

import java.time.LocalDate;
import java.util.List;

/** DashboardResponseに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public record DashboardResponse(
    long todayStudySeconds,
    int currentStreakDays,
    int totalStudyDays,
    Long activeConversationId,
    List<DailyActivity> activities) {

  /** DailyActivityに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  public record DailyActivity(
      LocalDate date, int sessionCount, long messageCount, long studySeconds, int level) {}
}
