package com.talkon.speechservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InternalKeyInterceptor implements HandlerInterceptor {
  public static final String HEADER = "X-Internal-Service-Key";
  private final String expectedKey;

  public InternalKeyInterceptor(@Value("${app.internal-key}") String expectedKey) {
    this.expectedKey = expectedKey;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (expectedKey.equals(request.getHeader(HEADER))) {
      return true;
    }
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    return false;
  }
}
