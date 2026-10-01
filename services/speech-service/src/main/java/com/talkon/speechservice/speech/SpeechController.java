package com.talkon.speechservice.speech;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/internal/speech")
public class SpeechController {
  private final SpeechProvider provider;

  public SpeechController(SpeechProvider provider) {
    this.provider = provider;
  }

  @PostMapping(value = "/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public TranscriptionResponse transcribe(@RequestPart("audio") MultipartFile audio)
      throws IOException {
    var result = provider.transcribe(audio.getBytes(), audio.getOriginalFilename());
    return new TranscriptionResponse(result.text(), result.model());
  }

  @PostMapping(value = "/synthesis", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<byte[]> synthesize(@Valid @RequestBody SynthesisRequest request) {
    var result = provider.synthesize(request.text());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, result.contentType())
        .header("X-Speech-Model", result.model())
        .body(result.bytes());
  }

  public record TranscriptionResponse(String text, String model) {}

  public record SynthesisRequest(@NotBlank @Size(max = 10_000) String text) {}
}
