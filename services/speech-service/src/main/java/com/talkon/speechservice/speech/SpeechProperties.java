package com.talkon.speechservice.speech;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.speech")
public record SpeechProperties(
    String sttModel,
    String ttsModel,
    String voice,
    double speed,
    String format,
    int timeoutSeconds,
    int retryCount) {}
