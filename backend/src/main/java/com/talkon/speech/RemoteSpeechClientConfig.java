// 音声認識・音声合成を独立サービスへ委譲します。会話BackendからOpenAI固有の通信処理を分離します。

package com.talkon.speech;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Configuration
public class RemoteSpeechClientConfig {
  static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";
  static final String MODEL_HEADER = "X-Speech-Model";

  @Bean
  SpeechRecognitionService speechRecognitionService(SpeechServiceProperties properties) {
    return new RemoteSpeechClient(properties);
  }

  @Bean
  TextToSpeechService textToSpeechService(SpeechServiceProperties properties) {
    return new RemoteSpeechClient(properties);
  }

  static class RemoteSpeechClient implements SpeechRecognitionService, TextToSpeechService {
    private final RestClient http;

    RemoteSpeechClient(SpeechServiceProperties properties) {
      var requestFactory = new SimpleClientHttpRequestFactory();
      var timeout = Duration.ofSeconds(properties.timeoutSeconds());
      requestFactory.setConnectTimeout(timeout);
      requestFactory.setReadTimeout(timeout);
      http =
          RestClient.builder()
              .requestFactory(requestFactory)
              .baseUrl(properties.baseUrl())
              .defaultHeader(INTERNAL_KEY_HEADER, properties.internalKey())
              .build();
    }

    @Override
    public Transcription transcribe(byte[] audio, String contentType, String filename) {
      var body = new LinkedMultiValueMap<String, Object>();
      body.add("audio", namedResource(audio, filename));
      JsonNode response =
          http.post()
              .uri("/internal/speech/transcriptions")
              .contentType(MediaType.MULTIPART_FORM_DATA)
              .body(body)
              .retrieve()
              .body(JsonNode.class);
      if (response == null || response.path("text").asText().isBlank()) {
        throw new IllegalStateException("Speech service returned an empty transcription");
      }
      return new Transcription(
          response.path("text").asText(), response.path("model").asText(), audio.length);
    }

    @Override
    public SpeechAudio synthesize(String text) {
      var response =
          http.post()
              .uri("/internal/speech/synthesis")
              .contentType(MediaType.APPLICATION_JSON)
              .body(new SynthesisRequest(text))
              .retrieve()
              .toEntity(byte[].class);
      byte[] bytes = response.getBody();
      if (bytes == null || bytes.length == 0) {
        throw new IllegalStateException("Speech service returned empty audio");
      }
      String contentType =
          response.getHeaders().getContentType() == null
              ? MediaType.APPLICATION_OCTET_STREAM_VALUE
              : response.getHeaders().getContentType().toString();
      return new SpeechAudio(bytes, contentType, response.getHeaders().getFirst(MODEL_HEADER));
    }

    private static ByteArrayResource namedResource(byte[] audio, String filename) {
      return new ByteArrayResource(audio) {
        @Override
        public String getFilename() {
          return filename == null || filename.isBlank() ? "recording.webm" : filename;
        }
      };
    }

    private record SynthesisRequest(String text) {}
  }
}
