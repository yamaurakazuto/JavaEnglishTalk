// 音声データを英文へ変換する境界です。会話処理から外部STT APIの形式を分離するために定義します。

package com.kazuto.talkon.speech;

public interface SpeechRecognitionService {
  Transcription transcribe(byte[] audio, String contentType, String filename);

  record Transcription(String text, String model, long inputBytes) {}
}
