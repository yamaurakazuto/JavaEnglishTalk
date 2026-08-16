// ローカル接続を開発ユーザーとして認証します。本番のパスワード認証を変えずに開発時の入力だけを省略するためのFilterです。

package com.kazuto.talkon.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class LocalAutoLoginFilter extends OncePerRequestFilter {
  private final boolean enabled;
  private final LocalAutoLoginService localAutoLoginService;

  public LocalAutoLoginFilter(boolean enabled, LocalAutoLoginService localAutoLoginService) {
    this.enabled = enabled;
    this.localAutoLoginService = localAutoLoginService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (enabled
        && isLoopbackAddress(request.getRemoteAddr())
        && SecurityContextHolder.getContext().getAuthentication() == null) {
      var user = localAutoLoginService.getOrCreateUser();
      var principal = new TalkOnPrincipal(user.getId(), user.getEmail(), user.getDisplayName());
      var authentication =
          UsernamePasswordAuthenticationToken.authenticated(
              principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }

  private boolean isLoopbackAddress(String remoteAddress) {
    try {
      return InetAddress.getByName(remoteAddress).isLoopbackAddress();
    } catch (UnknownHostException exception) {
      return false;
    }
  }
}
