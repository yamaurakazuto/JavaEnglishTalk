// 音声会話のmultipart受付と音声再生成APIを提供します。HTTP処理を音声ユースケースから分離します。

package com.talkon.speech;

import com.talkon.auth.CurrentUser;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** VoiceConversationControllerに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
@RestController
@RequestMapping("/api/conversations/{conversationId}")
public class VoiceConversationController {
  private final VoiceConversationService service;

  /** VoiceConversationControllerを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  public VoiceConversationController(VoiceConversationService service) {
    this.service = service;
  }

  /** voice turnに対応する処理を実行します。 画面やHTTPリクエストから対象のユースケースを安全に利用できるようにするために必要です。 */
  @PostMapping(value = "/voice-turns", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public VoiceConversationService.VoiceTurnResult voiceTurn(
      @PathVariable Long conversationId,
      @RequestPart("audio") MultipartFile audio,
      Authentication authentication)
      throws java.io.IOException {
    return service.send(
        conversationId,
        CurrentUser.require(authentication).id(),
        audio.getBytes(),
        audio.getContentType(),
        audio.getOriginalFilename());
  }

  /** speechに対応する処理を実行します。 画面やHTTPリクエストから対象のユースケースを安全に利用できるようにするために必要です。 */
  @PostMapping("/messages/{messageId}/speech")
  public ResponseEntity<byte[]> speech(
      @PathVariable Long conversationId,
      @PathVariable Long messageId,
      Authentication authentication) {
    var audio =
        service.synthesizeMessage(
            conversationId, messageId, CurrentUser.require(authentication).id());
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.CONTENT_TYPE, audio.contentType())
        .body(audio.bytes());
  }
}
