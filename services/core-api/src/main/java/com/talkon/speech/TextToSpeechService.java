// 英文を再生可能な音声へ変換する境界です。会話処理から外部TTS APIの形式を分離するために定義します。

package com.talkon.speech;

/** TextToSpeechServiceに関する責務をまとめるインターフェースです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public interface TextToSpeechService {
  /** AIの英文を再生可能な音声データへ変換します。 音声会話処理を特定の音声合成APIから分離するために必要です。 */
  SpeechAudio synthesize(String text);

  /** SpeechAudioに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  record SpeechAudio(byte[] bytes, String contentType, String model) {}
}
