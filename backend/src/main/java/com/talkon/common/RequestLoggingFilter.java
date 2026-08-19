// APIリクエストの識別子・結果・処理時間を記録します。障害発生時に同じ処理のログを追跡するためのFilterです。

package com.talkon.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** RequestLoggingFilterに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  /** do filter internalに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = request.getHeader("X-Request-ID");
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }
    long startedAt = System.nanoTime();
    MDC.put("requestId", requestId);
    response.setHeader("X-Request-ID", requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      log.info(
          "requestId={} method={} path={} status={} durationMs={}",
          requestId,
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          (System.nanoTime() - startedAt) / 1_000_000);
      MDC.remove("requestId");
    }
  }
}
