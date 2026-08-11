// STTとTTSの交換可能な設定値を保持します。モデル名や音声制限をコードへ埋め込まないための設定型です。

package com.kazuto.talkon.speech;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
