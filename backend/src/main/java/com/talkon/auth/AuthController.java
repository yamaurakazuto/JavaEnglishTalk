// 登録・ログイン・ログアウト・本人確認のHTTP APIを提供します。認証ユースケースの境界を明確にするためのControllerです。

package com.talkon.auth;

import com.talkon.common.ApiException;
import com.talkon.user.EnglishLevel;
import com.talkon.user.User;
import com.talkon.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final AuthenticationManager manager;

  public AuthController(UserRepository u, PasswordEncoder e, AuthenticationManager m) {
    users = u;
    encoder = e;
    manager = m;
  }

  public record RegisterRequest(
      @NotBlank @Size(max = 50) String displayName,
      @NotBlank @Email @Size(max = 255) String email,
      @NotBlank @Size(min = 8, max = 72) String password) {}

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

  public record UserResponse(
      Long id, String displayName, String email, EnglishLevel englishLevel) {}

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public UserResponse register(@Valid @RequestBody RegisterRequest r) {
    var email = r.email().trim().toLowerCase(Locale.ROOT);
    var name = r.displayName().trim();
    if (name.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "表示名を入力してください。");
    }
    if (users.existsByEmail(email)) {
      throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "このメールアドレスは登録済みです。");
    }
    return response(users.save(new User(name, email, encoder.encode(r.password()))));
  }

  @PostMapping("/login")
  public UserResponse login(
      @Valid @RequestBody LoginRequest r, HttpServletRequest req, HttpServletResponse res) {
    try {
      var token =
          manager.authenticate(
              UsernamePasswordAuthenticationToken.unauthenticated(
                  r.email().trim().toLowerCase(Locale.ROOT), r.password()));
      var u = users.findByEmail(token.getName()).orElseThrow();
      var auth =
          UsernamePasswordAuthenticationToken.authenticated(
              new TalkOnPrincipal(u.getId(), u.getEmail(), u.getDisplayName()),
              null,
              token.getAuthorities());
      var context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(auth);
      SecurityContextHolder.setContext(context);
      new HttpSessionSecurityContextRepository().saveContext(context, req, res);
      return response(u);
    } catch (AuthenticationException ex) {
      throw new ApiException(
          HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "メールアドレスまたはパスワードが違います。");
    }
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(HttpServletRequest req, HttpServletResponse res) {
    var session = req.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    SecurityContextHolder.clearContext();
    var cookie = new jakarta.servlet.http.Cookie("JSESSIONID", "");
    cookie.setPath("/");
    cookie.setMaxAge(0);
    cookie.setHttpOnly(true);
    res.addCookie(cookie);
  }

  @GetMapping("/me")
  public UserResponse me(Authentication a) {
    var p = CurrentUser.require(a);
    return response(users.findById(p.id()).orElseThrow());
  }

  private static UserResponse response(User u) {
    return new UserResponse(u.getId(), u.getDisplayName(), u.getEmail(), u.getEnglishLevel());
  }
}
