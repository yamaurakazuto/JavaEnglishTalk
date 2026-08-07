// 会話履歴からダッシュボード用の数値を集計します。MVPでは分かりやすさを優先してJava上で単純に計算します。

package com.kazuto.talkon.dashboard;

import com.kazuto.talkon.conversation.ConversationMessageRepository;
import com.kazuto.talkon.conversation.ConversationSession;
import com.kazuto.talkon.conversation.ConversationSessionRepository;
import com.kazuto.talkon.conversation.ConversationStatus;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
  private final ConversationSessionRepository sessions;
  private final ConversationMessageRepository messages;

  public DashboardService(
      ConversationSessionRepository sessions, ConversationMessageRepository messages) {
    this.sessions = sessions;
    this.messages = messages;
  }

  @Transactional(readOnly = true)
  public DashboardResponse getDashboard(Long userId) {
    ZoneId zone = ZoneId.systemDefault();
    LocalDate today = LocalDate.now(zone);
    LocalDate firstDay =
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).minusWeeks(51);
    Instant firstInstant = firstDay.atStartOfDay(zone).toInstant();
    Instant now = Instant.now();

    var recentSessions =
        sessions.findByUserIdAndStartedAtGreaterThanEqualOrderByStartedAtAsc(userId, firstInstant);
    Map<LocalDate, DayTotal> totals = new HashMap<>();

    for (ConversationSession session : recentSessions) {
      LocalDate date = session.getStartedAt().atZone(zone).toLocalDate();
      Instant end = session.getFinishedAt() == null ? now : session.getFinishedAt();
      long seconds = Math.max(0, Duration.between(session.getStartedAt(), end).getSeconds());
      long messageCount = messages.countBySessionId(session.getId());
      totals.computeIfAbsent(date, ignored -> new DayTotal()).add(seconds, messageCount);
    }

    var activities = new ArrayList<DashboardResponse.DailyActivity>();
    Set<LocalDate> studyDays = new HashSet<>();
    for (LocalDate date = firstDay; !date.isAfter(today); date = date.plusDays(1)) {
      DayTotal total = totals.getOrDefault(date, new DayTotal());
      if (total.sessionCount > 0) {
        studyDays.add(date);
      }
      activities.add(
          new DashboardResponse.DailyActivity(
              date,
              total.sessionCount,
              total.messageCount,
              total.studySeconds,
              level(total.sessionCount)));
    }

    int streak = countStreak(today, studyDays);
    long todaySeconds = totals.getOrDefault(today, new DayTotal()).studySeconds;
    Long activeId =
        sessions
            .findFirstByUserIdAndStatusOrderByStartedAtDesc(userId, ConversationStatus.ACTIVE)
            .map(ConversationSession::getId)
            .orElse(null);

    return new DashboardResponse(todaySeconds, streak, studyDays.size(), activeId, activities);
  }

  private int countStreak(LocalDate today, Set<LocalDate> studyDays) {
    int streak = 0;
    LocalDate date = today;
    while (studyDays.contains(date)) {
      streak++;
      date = date.minusDays(1);
    }
    return streak;
  }

  private int level(int sessionCount) {
    return Math.min(4, sessionCount);
  }

  private static class DayTotal {
    private int sessionCount;
    private long messageCount;
    private long studySeconds;

    void add(long seconds, long messages) {
      sessionCount++;
      messageCount += messages;
      studySeconds += seconds;
    }
  }
}
