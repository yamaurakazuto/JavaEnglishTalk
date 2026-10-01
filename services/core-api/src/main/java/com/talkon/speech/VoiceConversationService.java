// STT、会話生成、TTSを1ターンとして調整します。各処理を交換可能なまま部分成功と計測を扱うServiceです。

package com.talkon.speech;

import com.talkon.common.ApiException;
import com.talkon.conversation.ConversationDtos;
import com.talkon.conversation.ConversationService;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** VoiceConversationServiceに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
@Service
public class VoiceConversationService {
  private static final Logger log = LoggerFactory.getLogger(VoiceConversationService.class);
  private static final Set<String> SUPPORTED_TYPES =
      Set.of("audio/webm", "audio/wav", "audio/x-wav", "audio/mpeg", "audio/mp4", "audio/x-m4a");

  private final ConversationService conversations;
  private final SpeechRecognitionService recognition;
  private final TextToSpeechService speech;
  private final SpeechProperties properties;

  /** VoiceConversationServiceを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  public VoiceConversationService(
      ConversationService conversations,
      @Qualifier("speechRecognitionService") SpeechRecognitionService recognition,
      @Qualifier("textToSpeechService") TextToSpeechService speech,
      SpeechProperties properties) {
    this.conversations = conversations;
    this.recognition = recognition;
    this.speech = speech;
    this.properties = properties;
  }

  /** sendに対応する処理を実行します。 画面やHTTPリクエストから対象のユースケースを安全に利用できるようにするために必要です。 */
  public VoiceTurnResult send(
      Long conversationId, Long userId, byte[] audio, String contentType, String filename) {
    validate(audio, contentType);
    Instant totalStarted = Instant.now();

    SpeechRecognitionService.Transcription transcription;
    long sttMillis;
    try {
      Instant started = Instant.now();
      transcription = recognition.transcribe(audio, contentType, filename);
      sttMillis = elapsed(started);
      logStep(userId, conversationId, "STT", "COMPLETED", sttMillis, transcription.model());
    } catch (Exception exception) {
      logStep(userId, conversationId, "STT", "FAILED", elapsed(totalStarted), "unknown");
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "STT_UNAVAILABLE",
          "音声を文字に変換できませんでした。録音内容を確認して再試行してください。");
    }

    ConversationDtos.Detail conversation;
    long llmMillis;
    try {
      Instant started = Instant.now();
      conversation = conversations.send(conversationId, userId, transcription.text());
      llmMillis = elapsed(started);
      logStep(userId, conversationId, "LLM", "COMPLETED", llmMillis, "configured-llm");
    } catch (ApiException exception) {
      logStep(userId, conversationId, "LLM", "FAILED", elapsed(totalStarted), "configured-llm");
      throw exception;
    }

    var assistant = conversation.messages().getLast();
    long ttsMillis;
    String audioBase64 = null;
    String audioContentType = null;
    String warning = null;
    try {
      Instant started = Instant.now();
      var generated = speech.synthesize(assistant.content());
      ttsMillis = elapsed(started);
      audioBase64 = Base64.getEncoder().encodeToString(generated.bytes());
      audioContentType = generated.contentType();
      logStep(userId, conversationId, "TTS", "COMPLETED", ttsMillis, generated.model());
    } catch (Exception exception) {
      ttsMillis = 0;
      warning = "音声の生成に失敗しました。英文はそのまま利用できます。";
      logStep(userId, conversationId, "TTS", "FAILED", elapsed(totalStarted), "unknown");
    }

    long totalMillis = elapsed(totalStarted);
    log.info(
        "userId={} conversationId={} action=voiceTurn status=COMPLETED totalMs={} inputBytes={} transcriptCharacters={} assistantCharacters={}",
        userId,
        conversationId,
        totalMillis,
        transcription.inputBytes(),
        transcription.text().length(),
        assistant.content().length());
    return new VoiceTurnResult(
        transcription.text(),
        conversation,
        audioBase64,
        audioContentType,
        new ProcessingTimes(sttMillis, llmMillis, ttsMillis, totalMillis),
        warning);
  }

  /** synthesize messageに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  public TextToSpeechService.SpeechAudio synthesizeMessage(
      Long conversationId, Long messageId, Long userId) {
    var conversation = conversations.detail(conversationId, userId);
    var message =
        conversation.messages().stream()
            .filter(item -> item.id().equals(messageId) && item.role().equals("ASSISTANT"))
            .findFirst()
            .orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "AIメッセージが見つかりません。"));
    try {
      return speech.synthesize(message.content());
    } catch (Exception exception) {
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE, "TTS_UNAVAILABLE", "音声を生成できませんでした。再試行してください。");
    }
  }

  /** validateによって対象の状態や処理を更新します。 状態変更のルールを一箇所に集約し、不整合を防ぐために必要です。 */
  private void validate(byte[] audio, String contentType) {
    if (audio == null || audio.length < 512) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "AUDIO_TOO_SHORT", "録音が短すぎるか無音です。もう一度話してください。");
    }
    if (audio.length > properties.maxBytes()) {
      throw new ApiException(
          HttpStatus.PAYLOAD_TOO_LARGE, "AUDIO_TOO_LARGE", "音声ファイルの上限サイズを超えています。");
    }
    String normalized = contentType == null ? "" : contentType.split(";", 2)[0].toLowerCase();
    if (!SUPPORTED_TYPES.contains(normalized)) {
      throw new ApiException(
          HttpStatus.UNSUPPORTED_MEDIA_TYPE,
          "UNSUPPORTED_AUDIO_TYPE",
          "この音声形式には対応していません。webm、wav、mp3、mp4、m4aを利用してください。");
    }
  }

  /** elapsedに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  private static long elapsed(Instant started) {
    return Duration.between(started, Instant.now()).toMillis();
  }

  /** log stepに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  private static void logStep(
      Long userId,
      Long conversationId,
      String step,
      String status,
      long durationMillis,
      String model) {
    log.info(
        "userId={} conversationId={} action=voiceStep step={} status={} durationMs={} model={}",
        userId,
        conversationId,
        step,
        status,
        durationMillis,
        model);
  }

  /** ProcessingTimesに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  public record ProcessingTimes(long sttMs, long llmMs, long ttsMs, long totalMs) {}

  /** VoiceTurnResultに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  public record VoiceTurnResult(
      String userTranscript,
      ConversationDtos.Detail conversation,
      String assistantAudioBase64,
      String audioContentType,
      ProcessingTimes processingTimes,
      String warning) {}
}
