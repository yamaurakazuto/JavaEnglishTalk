package com.talkon.speechservice.speech;

public interface SpeechProvider {
  Transcription transcribe(byte[] audio, String filename);

  Audio synthesize(String text);

  record Transcription(String text, String model) {}

  record Audio(byte[] bytes, String contentType, String model) {}
}
