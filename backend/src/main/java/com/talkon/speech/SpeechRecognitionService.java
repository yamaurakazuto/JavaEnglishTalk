// 音声データを英文へ変換する境界です。会話処理から外部STT APIの形式を分離するために定義します。

package com.talkon.speech;

/** SpeechRecognitionServiceに関する責務をまとめるインターフェースです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public interface SpeechRecognitionService {
  /** 録音データを英語テキストへ変換します。 音声会話処理を特定の音声認識APIから分離するために必要です。 */
  Transcription transcribe(byte[] audio, String contentType, String filename);

  /** Transcriptionに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  record Transcription(String text, String model, long inputBytes) {}
}
