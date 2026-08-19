// 会話開始・送信・終了・履歴のHTTP APIを提供します。HTTP変換と会話ユースケースを分離するためのControllerです。

package com.talkon.conversation;

import com.talkon.auth.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** ConversationControllerに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
  private final ConversationService service;

  /** ConversationControllerを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  public ConversationController(ConversationService s) {
    service = s;
  }

  /** MessageRequestに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  public record MessageRequest(@NotBlank @Size(max = 2000) String content) {}

  /** WordTranslationRequestに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  public record WordTranslationRequest(@NotBlank @Size(max = 60) String word) {}

  /** startに対応する処理を実行します。 画面やHTTPリクエストから対象のユースケースを安全に利用できるようにするために必要です。 */
  @PostMapping
  public ResponseEntity<ConversationDtos.Detail> start(Authentication a) {
    var result = service.start(CurrentUser.require(a).id());
    return ResponseEntity.status(result.created() ? 201 : 200).body(result.detail());
  }

  /** activeに対応する処理を実行します。 画面やHTTPリクエストから対象のユースケースを安全に利用できるようにするために必要です。 */
  @GetMapping("/active")
  public ResponseEntity<ConversationDtos.Detail> active(Authentication a) {
    var d = service.active(CurrentUser.require(a).id());
    return d == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(d);
  }

  /** detailに対応する処理を実行します。 画面やHTTPリクエストから対象のユースケースを安全に利用できるようにするために必要です。 */
  @GetMapping("/{id}")
  public ConversationDtos.Detail detail(@PathVariable Long id, Authentication a) {
    return service.detail(id, CurrentUser.require(a).id());
  }

  /** sendに対応する処理を実行します。 画面やHTTPリクエストから対象のユースケースを安全に利用できるようにするために必要です。 */
  @PostMapping("/{id}/messages")
  public ConversationDtos.Detail send(
      @PathVariable Long id, @Valid @RequestBody MessageRequest r, Authentication a) {
    return service.send(id, CurrentUser.require(a).id(), r.content());
  }

  /** finishに対応する処理を実行します。 画面やHTTPリクエストから対象のユースケースを安全に利用できるようにするために必要です。 */
  @PostMapping("/{id}/finish")
  public ResponseEntity<ConversationDtos.Detail> finish(@PathVariable Long id, Authentication a) {
    return ResponseEntity.accepted().body(service.finish(id, CurrentUser.require(a).id()));
  }

  /** retry feedbackによって対象の状態や処理を更新します。 状態変更のルールを一箇所に集約し、不整合を防ぐために必要です。 */
  @PostMapping("/{id}/feedback/retry")
  public ResponseEntity<ConversationDtos.Detail> retryFeedback(
      @PathVariable Long id, Authentication a) {
    return ResponseEntity.accepted().body(service.retryFeedback(id, CurrentUser.require(a).id()));
  }

  /** translateの外部サービスまたは代替処理を実行します。 AI・音声機能の詳細を呼び出し側から分離し、実装を交換可能にするために必要です。 */
  @PostMapping("/{id}/messages/{messageId}/translation")
  public ConversationDtos.MessageResponse translate(
      @PathVariable Long id, @PathVariable Long messageId, Authentication a) {
    return service.translate(id, messageId, CurrentUser.require(a).id());
  }

  /** translate wordの外部サービスまたは代替処理を実行します。 AI・音声機能の詳細を呼び出し側から分離し、実装を交換可能にするために必要です。 */
  @PostMapping("/{id}/messages/{messageId}/word-translation")
  public ConversationDtos.WordTranslationResponse translateWord(
      @PathVariable Long id,
      @PathVariable Long messageId,
      @Valid @RequestBody WordTranslationRequest request,
      Authentication authentication) {
    return service.translateWord(
        id, messageId, CurrentUser.require(authentication).id(), request.word());
  }

  /** historyに対応する処理を実行します。 画面やHTTPリクエストから対象のユースケースを安全に利用できるようにするために必要です。 */
  @GetMapping
  public ConversationDtos.PageResponse history(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      Authentication a) {
    return service.history(CurrentUser.require(a).id(), page, size);
  }
}
