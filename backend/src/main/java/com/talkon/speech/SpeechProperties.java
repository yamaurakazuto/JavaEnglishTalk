// STTとTTSの交換可能な設定値を保持します。モデル名や音声制限をコードへ埋め込まないための設定型です。

package com.talkon.speech;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** SpeechPropertiesに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
@ConfigurationProperties("app.speech")
public record SpeechProperties(
    String sttModel,
    String ttsModel,
    String voice,
    double speed,
    String format,
    int timeoutSeconds,
    int retryCount,
    long maxBytes,
    int maxRecordingSeconds) {}
