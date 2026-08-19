// ローカル接続を開発ユーザーとして認証します。本番のパスワード認証を変えずに開発時の入力だけを省略するためのFilterです。

package com.talkon.auth;

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

/** LocalAutoLoginFilterに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public class LocalAutoLoginFilter extends OncePerRequestFilter {
  private final boolean enabled;
  private final LocalAutoLoginService localAutoLoginService;

  /** LocalAutoLoginFilterを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  public LocalAutoLoginFilter(boolean enabled, LocalAutoLoginService localAutoLoginService) {
    this.enabled = enabled;
    this.localAutoLoginService = localAutoLoginService;
  }

  /** do filter internalに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
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

  /** is loopback addressの条件を満たすか判定します。 条件判断を呼び出し側へ重複させず、一貫した結果を返すために必要です。 */
  private boolean isLoopbackAddress(String remoteAddress) {
    try {
      return InetAddress.getByName(remoteAddress).isLoopbackAddress();
    } catch (UnknownHostException exception) {
      return false;
    }
  }
}
