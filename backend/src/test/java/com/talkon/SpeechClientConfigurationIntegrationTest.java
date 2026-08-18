// 実API設定時の音声Service構成を統合検証します。STTとTTSのBean候補が競合して起動できなくなる問題を防ぐテストです。

package com.talkon;

import static org.assertj.core.api.Assertions.assertThat;

import com.talkon.speech.VoiceConversationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.llm.api-key=test-api-key")
class SpeechClientConfigurationIntegrationTest {
  @Autowired VoiceConversationService voiceConversationService;

  @Test
  void applicationStartsWithOpenAiSpeechClients() {
    assertThat(voiceConversationService).isNotNull();
  }
}
