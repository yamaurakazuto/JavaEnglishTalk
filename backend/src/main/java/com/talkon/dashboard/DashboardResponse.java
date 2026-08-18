// ダッシュボードに必要な学習時間と継続情報をまとめます。画面が一度の通信で表示できるようにするResponse DTOです。

package com.talkon.dashboard;

import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
    long todayStudySeconds,
    int currentStreakDays,
    int totalStudyDays,
    Long activeConversationId,
    List<DailyActivity> activities) {

  public record DailyActivity(
      LocalDate date, int sessionCount, long messageCount, long studySeconds, int level) {}
}
