// 例外を共通APIエラーへ変換します。Controllerごとの重複したエラー処理を避けるために集約しています。

package com.kazuto.talkon.common;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<ApiError> api(ApiException e) {
    return ResponseEntity.status(e.status())
        .body(new ApiError(e.code(), e.getMessage(), null, null));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
    Map<String, String> fields = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(x -> fields.putIfAbsent(x.getField(), x.getDefaultMessage()));
    return ResponseEntity.badRequest()
        .body(new ApiError("VALIDATION_ERROR", "入力内容を確認してください。", fields, null));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiError> constraint() {
    return ResponseEntity.badRequest()
        .body(new ApiError("VALIDATION_ERROR", "入力内容を確認してください。", null, null));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiError> conflict() {
    return ResponseEntity.status(409).body(new ApiError("CONFLICT", "競合が発生しました。", null, null));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> other(Exception e) {
    return ResponseEntity.status(500)
        .body(new ApiError("INTERNAL_ERROR", "処理に失敗しました。", null, null));
  }
}
