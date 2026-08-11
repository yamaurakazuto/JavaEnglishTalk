// 音声ターンの部分失敗を検証します。TTS障害でも生成済み英文を失わない契約を守るための単体テストです。

package com.kazuto.talkon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kazuto.talkon.conversation.ConversationDtos;
import com.kazuto.talkon.conversation.ConversationService;
import com.kazuto.talkon.speech.SpeechProperties;
import com.kazuto.talkon.speech.SpeechRecognitionService;
import com.kazuto.talkon.speech.TextToSpeechService;
import com.kazuto.talkon.speech.VoiceConversationService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class VoiceConversationServiceTest {
  @Test
  void ttsFailureReturnsTextConversationAndWarning() {
    var conversations = mock(ConversationService.class);
    var recognition = mock(SpeechRecognitionService.class);
    var speech = mock(TextToSpeechService.class);
    var properties =
        new SpeechProperties("stt-model", "tts-model", "coral", 1.0, "mp3", 30, 1, 5_242_880, 60);
    var detail =
        new ConversationDtos.Detail(
            1L,
            "ACTIVE",
            Instant.now(),
            null,
            List.of(
                new ConversationDtos.MessageResponse(
                    2L, "ASSISTANT", "That sounds fun!", null, 3, Instant.now())),
            null);
    when(recognition.transcribe(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(
            new SpeechRecognitionService.Transcription("I play soccer.", "stt-model", 1_024));
    when(conversations.send(1L, 10L, "I play soccer.")).thenReturn(detail);
    when(speech.synthesize("That sounds fun!"))
        .thenThrow(new IllegalStateException("temporary failure"));

    var result =
        new VoiceConversationService(conversations, recognition, speech, properties)
            .send(1L, 10L, new byte[1_024], "audio/webm", "recording.webm");

    assertThat(result.conversation()).isEqualTo(detail);
    assertThat(result.assistantAudioBase64()).isNull();
    assertThat(result.warning()).contains("英文はそのまま利用できます");
  }
}
