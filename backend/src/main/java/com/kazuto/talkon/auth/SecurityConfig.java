// セッション認証・CSRF・CORS・例外応答を設定します。セキュリティ方針を一箇所で管理するための設定です。

package com.kazuto.talkon.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazuto.talkon.common.ApiError;
import com.kazuto.talkon.user.UserRepository;
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

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService users(UserRepository repository) {
    return email ->
        repository
            .findByEmail(email.trim().toLowerCase())
            .map(
                u ->
                    new UserDetails() {
                      public String getUsername() {
                        return u.getEmail();
                      }

                      public String getPassword() {
                        return u.getPasswordHash();
                      }

                      public List<SimpleGrantedAuthority> getAuthorities() {
                        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
                      }

                      public boolean isAccountNonExpired() {
                        return true;
                      }

                      public boolean isAccountNonLocked() {
                        return true;
                      }

                      public boolean isCredentialsNonExpired() {
                        return true;
                      }

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

  private static void write(
      HttpServletResponse res, ObjectMapper mapper, int status, String code, String message)
      throws java.io.IOException {
    res.setStatus(status);
    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
    mapper.writeValue(res.getOutputStream(), new ApiError(code, message, null, null));
  }
}
