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

@RestController
@RequestMapping("/api/conversations/{conversationId}")
public class VoiceConversationController {
  private final VoiceConversationService service;

  public VoiceConversationController(VoiceConversationService service) {
    this.service = service;
  }

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
