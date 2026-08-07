// 会話開始・送信・終了・履歴のHTTP APIを提供します。HTTP変換と会話ユースケースを分離するためのControllerです。

package com.kazuto.talkon.conversation;

import com.kazuto.talkon.auth.CurrentUser;
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

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
  private final ConversationService service;

  public ConversationController(ConversationService s) {
    service = s;
  }

  public record MessageRequest(@NotBlank @Size(max = 2000) String content) {}

  @PostMapping
  public ResponseEntity<ConversationDtos.Detail> start(Authentication a) {
    var result = service.start(CurrentUser.require(a).id());
    return ResponseEntity.status(result.created() ? 201 : 200).body(result.detail());
  }

  @GetMapping("/active")
  public ResponseEntity<ConversationDtos.Detail> active(Authentication a) {
    var d = service.active(CurrentUser.require(a).id());
    return d == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(d);
  }

  @GetMapping("/{id}")
  public ConversationDtos.Detail detail(@PathVariable Long id, Authentication a) {
    return service.detail(id, CurrentUser.require(a).id());
  }

  @PostMapping("/{id}/messages")
  public ConversationDtos.Detail send(
      @PathVariable Long id, @Valid @RequestBody MessageRequest r, Authentication a) {
    return service.send(id, CurrentUser.require(a).id(), r.content());
  }

  @PostMapping("/{id}/finish")
  public ResponseEntity<ConversationDtos.Detail> finish(@PathVariable Long id, Authentication a) {
    return ResponseEntity.accepted().body(service.finish(id, CurrentUser.require(a).id()));
  }

  @PostMapping("/{id}/feedback/retry")
  public ResponseEntity<ConversationDtos.Detail> retryFeedback(
      @PathVariable Long id, Authentication a) {
    return ResponseEntity.accepted().body(service.retryFeedback(id, CurrentUser.require(a).id()));
  }

  @PostMapping("/{id}/messages/{messageId}/translation")
  public ConversationDtos.MessageResponse translate(
      @PathVariable Long id, @PathVariable Long messageId, Authentication a) {
    return service.translate(id, messageId, CurrentUser.require(a).id());
  }

  @GetMapping
  public ConversationDtos.PageResponse history(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      Authentication a) {
    return service.history(CurrentUser.require(a).id(), page, size);
  }
}
