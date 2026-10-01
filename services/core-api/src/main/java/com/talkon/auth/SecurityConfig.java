// セッション認証・CSRF・CORS・例外応答を設定します。セキュリティ方針を一箇所で管理するための設定です。

package com.talkon.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talkon.common.ApiError;
import com.talkon.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** SecurityConfigに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
@Configuration
public class SecurityConfig {
  /** password encoderに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** usersに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  @Bean
  UserDetailsService users(UserRepository repository) {
    return email ->
        repository
            .findByEmail(email.trim().toLowerCase())
            .map(
                u ->
                    new UserDetails() {
                      /** get usernameとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
                      public String getUsername() {
                        return u.getEmail();
                      }

                      /** get passwordとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
                      public String getPassword() {
                        return u.getPasswordHash();
                      }

                      /** get authoritiesとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
                      public List<SimpleGrantedAuthority> getAuthorities() {
                        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
                      }

                      /** is account non expiredの条件を満たすか判定します。 条件判断を呼び出し側へ重複させず、一貫した結果を返すために必要です。 */
                      public boolean isAccountNonExpired() {
                        return true;
                      }

                      /** is account non lockedの条件を満たすか判定します。 条件判断を呼び出し側へ重複させず、一貫した結果を返すために必要です。 */
                      public boolean isAccountNonLocked() {
                        return true;
                      }

                      /**
                       * is credentials non expiredの条件を満たすか判定します。 条件判断を呼び出し側へ重複させず、一貫した結果を返すために必要です。
                       */
                      public boolean isCredentialsNonExpired() {
                        return true;
                      }

                      /** is enabledの条件を満たすか判定します。 条件判断を呼び出し側へ重複させず、一貫した結果を返すために必要です。 */
                      public boolean isEnabled() {
                        return true;
                      }
                    })
            .orElseThrow(
                () ->
                    new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "user"));
  }

  @Bean
  DaoAuthenticationProvider authenticationProvider(
      UserDetailsService uds, PasswordEncoder encoder) {
    var p = new DaoAuthenticationProvider();
    p.setUserDetailsService(uds);
    p.setPasswordEncoder(encoder);
    return p;
  }

  /** authentication managerに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
    return c.getAuthenticationManager();
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      UserRepository users,
      ObjectMapper mapper,
      LocalAutoLoginService localAutoLoginService,
      @Value("${app.local-auto-login:false}") boolean localAutoLoginEnabled,
      @Value("${app.cors-origin}") String origin)
      throws Exception {
    var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrf.setCookiePath("/");
    var cors = new CorsConfiguration();
    cors.setAllowedOrigins(List.of(origin));
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
    cors.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
    cors.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors);
    return http.csrf(c -> c.csrfTokenRepository(csrf))
        .cors(c -> c.configurationSource(source))
        .addFilterBefore(
            new LocalAutoLoginFilter(localAutoLoginEnabled, localAutoLoginService),
            AnonymousAuthenticationFilter.class)
        .formLogin(c -> c.disable())
        .httpBasic(c -> c.disable())
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/csrf",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/v3/api-docs/**",
                        "/swagger-ui/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> write(res, mapper, 401, "UNAUTHORIZED", "ログインが必要です。"))
                    .accessDeniedHandler(
                        (req, res, ex) -> write(res, mapper, 403, "FORBIDDEN", "アクセスできません。")))
        .build();
  }

  /** writeに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  private static void write(
      HttpServletResponse res, ObjectMapper mapper, int status, String code, String message)
      throws java.io.IOException {
    res.setStatus(status);
    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
    mapper.writeValue(res.getOutputStream(), new ApiError(code, message, null, null));
  }
}
