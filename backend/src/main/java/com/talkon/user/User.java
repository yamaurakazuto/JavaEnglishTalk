// 利用者の表示名・メール・パスワードハッシュを永続化します。認証主体を表現するEntityです。

package com.talkon.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Userに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "display_name", nullable = false, length = 50)
  private String displayName;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "english_level", length = 20)
  private EnglishLevel englishLevel;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Userを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  protected User() {}

  /** Userを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  public User(String displayName, String email, String passwordHash) {
    this.displayName = displayName;
    this.email = email;
    this.passwordHash = passwordHash;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  /** get idとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public Long getId() {
    return id;
  }

  /** get display nameとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public String getDisplayName() {
    return displayName;
  }

  /** get emailとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public String getEmail() {
    return email;
  }

  /** get password hashとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public String getPasswordHash() {
    return passwordHash;
  }

  /** get english levelとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public EnglishLevel getEnglishLevel() {
    return englishLevel;
  }

  /** select english levelによって対象の状態や処理を更新します。 状態変更のルールを一箇所に集約し、不整合を防ぐために必要です。 */
  public void selectEnglishLevel(EnglishLevel level) {
    englishLevel = level;
    updatedAt = Instant.now();
  }
}
