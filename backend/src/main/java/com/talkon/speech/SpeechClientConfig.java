// FakeまたはOpenAI音声サービスを構成します。開発環境と実APIを同じインターフェースで切り替える設定です。

package com.talkon.speech;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Configuration
public class SpeechClientConfig {
  @Bean
  SpeechRecognitionService speechRecognitionService(
      @Value("${app.llm.api-key:}") String apiKey,
      @Value("${app.llm.base-url}") String baseUrl,
      SpeechProperties properties) {
    if (apiKey.isBlank()) {
      return (audio, contentType, filename) ->
          new SpeechRecognitionService.Transcription(
              "I like practicing English with TalkOn.", "local-stt", audio.length);
    }
    return new OpenAiSpeechClient(apiKey, baseUrl, properties);
  }

  @Bean
  TextToSpeechService textToSpeechService(
      @Value("${app.llm.api-key:}") String apiKey,
      @Value("${app.llm.base-url}") String baseUrl,
      SpeechProperties properties) {
    if (apiKey.isBlank()) {
      return text -> new TextToSpeechService.SpeechAudio(silentWav(), "audio/wav", "local-tts");
    }
    return new OpenAiSpeechClient(apiKey, baseUrl, properties);
  }

  private static byte[] silentWav() {
    int sampleRate = 16_000;
    int dataLength = sampleRate / 4 * 2;
    var wav = ByteBuffer.allocate(44 + dataLength).order(ByteOrder.LITTLE_ENDIAN);
    wav.put("RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    wav.putInt(36 + dataLength);
    wav.put("WAVEfmt ".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    wav.putInt(16).putShort((short) 1).putShort((short) 1);
    wav.putInt(sampleRate).putInt(sampleRate * 2).putShort((short) 2).putShort((short) 16);
    wav.put("data".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    wav.putInt(dataLength);
    return wav.array();
  }

  static class OpenAiSpeechClient implements SpeechRecognitionService, TextToSpeechService {
    private final RestClient http;
    private final SpeechProperties properties;

    OpenAiSpeechClient(String apiKey, String baseUrl, SpeechProperties properties) {
      var requestFactory = new SimpleClientHttpRequestFactory();
      requestFactory.setConnectTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
      requestFactory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
      this.http =
          RestClient.builder()
              .requestFactory(requestFactory)
              .baseUrl(baseUrl)
              .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
              .build();
      this.properties = properties;
    }

    @Override
    public Transcription transcribe(byte[] audio, String contentType, String filename) {
      var body = new LinkedMultiValueMap<String, Object>();
      body.add("model", properties.sttModel());
      body.add("language", "en");
      body.add("file", namedResource(audio, filename));
      JsonNode response =
          retry(
              () ->
                  http.post()
                      .uri("/audio/transcriptions")
                      .contentType(MediaType.MULTIPART_FORM_DATA)
                      .body(body)
                      .retrieve()
                      .body(JsonNode.class));
      String text = response == null ? "" : response.path("text").asText().trim();
      if (text.isEmpty()) {
        throw new IllegalStateException("Speech recognition returned empty text");
      }
      return new Transcription(text, properties.sttModel(), audio.length);
    }

    @Override
    public SpeechAudio synthesize(String text) {
      byte[] audio =
          retry(
              () ->
                  http.post()
                      .uri("/audio/speech")
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(
                          Map.of(
                              "model", properties.ttsModel(),
                              "voice", properties.voice(),
                              "input", text,
                              "speed", properties.speed(),
                              "response_format", properties.format()))
                      .retrieve()
                      .body(byte[].class));
      if (audio == null || audio.length == 0) {
        throw new IllegalStateException("Text-to-speech returned empty audio");
      }
      return new SpeechAudio(audio, mediaType(properties.format()), properties.ttsModel());
    }

    private <T> T retry(java.util.function.Supplier<T> call) {
      RuntimeException last = null;
      for (int attempt = 0; attempt <= properties.retryCount(); attempt++) {
        try {
          return call.get();
        } catch (RuntimeException exception) {
          last = exception;
        }
      }
      throw last == null ? new IllegalStateException("Speech API failed") : last;
    }

    private static ByteArrayResource namedResource(byte[] audio, String filename) {
      return new ByteArrayResource(audio) {
        @Override
        public String getFilename() {
          return filename == null || filename.isBlank() ? "recording.webm" : filename;
        }
      };
    }

    private static String mediaType(String format) {
      return switch (format) {
        case "wav" -> "audio/wav";
        case "opus" -> "audio/ogg";
        case "aac" -> "audio/aac";
        default -> "audio/mpeg";
      };
    }
  }
}
