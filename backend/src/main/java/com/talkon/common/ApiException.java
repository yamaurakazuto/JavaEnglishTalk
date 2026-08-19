// HTTPステータスと業務エラーコードを保持します。業務例外をHTTP変換から分離するための例外です。

package com.talkon.common;

import org.springframework.http.HttpStatus;

/** ApiExceptionに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public class ApiException extends RuntimeException {
  private final HttpStatus status;
  private final String code;

  /** ApiExceptionを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  public ApiException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  /** statusとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public HttpStatus status() {
    return status;
  }

  /** codeとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
  public String code() {
    return code;
  }
}
