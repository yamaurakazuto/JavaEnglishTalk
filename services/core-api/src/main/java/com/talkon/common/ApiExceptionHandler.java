// 例外を共通APIエラーへ変換します。Controllerごとの重複したエラー処理を避けるために集約しています。

package com.talkon.common;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** ApiExceptionHandlerに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  /** apiに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  @ExceptionHandler(ApiException.class)
  ResponseEntity<ApiError> api(ApiException e) {
    return ResponseEntity.status(e.status())
        .body(new ApiError(e.code(), e.getMessage(), null, null));
  }

  /** validationに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
    Map<String, String> fields = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(x -> fields.putIfAbsent(x.getField(), x.getDefaultMessage()));
    return ResponseEntity.badRequest()
        .body(new ApiError("VALIDATION_ERROR", "入力内容を確認してください。", fields, null));
  }

  /** constraintに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiError> constraint() {
    return ResponseEntity.badRequest()
        .body(new ApiError("VALIDATION_ERROR", "入力内容を確認してください。", null, null));
  }

  /** conflictに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiError> conflict() {
    return ResponseEntity.status(409).body(new ApiError("CONFLICT", "競合が発生しました。", null, null));
  }

  /** audio too largeに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ApiError> audioTooLarge() {
    return ResponseEntity.status(413)
        .body(new ApiError("AUDIO_TOO_LARGE", "音声ファイルの上限サイズを超えています。", null, null));
  }

  /** otherに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> other(Exception e) {
    log.error("action=handleApiException status=FAILED", e);
    return ResponseEntity.status(500)
        .body(new ApiError("INTERNAL_ERROR", "処理に失敗しました。", null, null));
  }
}
