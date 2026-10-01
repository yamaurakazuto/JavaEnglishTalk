// 独立した音声サービスへの接続設定を保持します。サービス間通信の宛先と認証情報を外部化するための設定型です。

package com.talkon.speech;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.speech-service")
public record SpeechServiceProperties(String baseUrl, String internalKey, int timeoutSeconds) {}
