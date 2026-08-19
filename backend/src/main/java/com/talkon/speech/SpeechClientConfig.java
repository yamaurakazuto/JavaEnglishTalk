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

/** SpeechClientConfigに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
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

  /** silent wavに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
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

  /** OpenAiSpeechClientに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  static class OpenAiSpeechClient implements SpeechRecognitionService, TextToSpeechService {
    private final RestClient http;
    private final SpeechProperties properties;

    /** OpenAiSpeechClientを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
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

    /** transcribeの外部サービスまたは代替処理を実行します。 AI・音声機能の詳細を呼び出し側から分離し、実装を交換可能にするために必要です。 */
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

    /** synthesizeの外部サービスまたは代替処理を実行します。 AI・音声機能の詳細を呼び出し側から分離し、実装を交換可能にするために必要です。 */
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

    /** retryによって対象の状態や処理を更新します。 状態変更のルールを一箇所に集約し、不整合を防ぐために必要です。 */
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

    /** named resourceに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
    private static ByteArrayResource namedResource(byte[] audio, String filename) {
      return new ByteArrayResource(audio) {
        /** get filenameとして保持している値を返します。 呼び出し側が内部状態を直接変更せず、安全に参照するために必要です。 */
        @Override
        public String getFilename() {
          return filename == null || filename.isBlank() ? "recording.webm" : filename;
        }
      };
    }

    /** media typeに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
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
