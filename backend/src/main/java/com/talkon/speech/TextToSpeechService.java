// 英文を再生可能な音声へ変換する境界です。会話処理から外部TTS APIの形式を分離するために定義します。

package com.talkon.speech;

public interface TextToSpeechService {
  SpeechAudio synthesize(String text);

  record SpeechAudio(byte[] bytes, String contentType, String model) {}
}
