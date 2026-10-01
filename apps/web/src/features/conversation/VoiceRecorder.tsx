/**
 * ブラウザ録音から音声会話API、AI音声の自動再生までを扱います。テキスト入力とは独立して失敗から復帰できる構成です。
 */

import { useEffect, useRef, useState } from "react";
import { api, Conversation, VoiceTurn } from "../../shared/api";

type VoiceRecorderProps = {
  conversationId: number;
  disabled: boolean;
  onConversationUpdated: (conversation: Conversation) => void;
  onError: (message: string) => void;
};

const MAX_RECORDING_SECONDS = 60;

export function VoiceRecorder({
  conversationId,
  disabled,
  onConversationUpdated,
  onError,
}: VoiceRecorderProps) {
  const recorder = useRef<MediaRecorder | undefined>(undefined);
  const stream = useRef<MediaStream | undefined>(undefined);
  const chunks = useRef<Blob[]>([]);
  const timer = useRef<number | undefined>(undefined);
  const [recording, setRecording] = useState(false);
  const [sending, setSending] = useState(false);
  const [seconds, setSeconds] = useState(0);
  const [transcript, setTranscript] = useState("");
  const [warning, setWarning] = useState("");
  const [audioUrl, setAudioUrl] = useState("");

  useEffect(
    () => () => {
      stream.current?.getTracks().forEach((track) => track.stop());
      if (timer.current) {
        window.clearInterval(timer.current);
      }
      if (audioUrl) {
        URL.revokeObjectURL(audioUrl);
      }
    },
    [audioUrl],
  );

  async function startRecording() {
    onError("");
    setWarning("");
    try {
      const mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: true,
      });
      const mimeType = MediaRecorder.isTypeSupported("audio/webm;codecs=opus")
        ? "audio/webm;codecs=opus"
        : "audio/mp4";
      const mediaRecorder = new MediaRecorder(mediaStream, { mimeType });
      stream.current = mediaStream;
      recorder.current = mediaRecorder;
      chunks.current = [];
      setSeconds(0);
      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          chunks.current.push(event.data);
        }
      };
      mediaRecorder.onstop = () => void submitRecording(mimeType);
      mediaRecorder.start();
      setRecording(true);
      timer.current = window.setInterval(() => {
        setSeconds((current) => {
          if (current + 1 >= MAX_RECORDING_SECONDS) {
            mediaRecorder.stop();
            return MAX_RECORDING_SECONDS;
          }
          return current + 1;
        });
      }, 1000);
    } catch {
      onError(
        "マイクを利用できません。ブラウザのマイク権限を確認してください。",
      );
    }
  }

  function stopRecording() {
    recorder.current?.stop();
  }

  async function submitRecording(mimeType: string) {
    setRecording(false);
    setSending(true);
    if (timer.current) {
      window.clearInterval(timer.current);
    }
    stream.current?.getTracks().forEach((track) => track.stop());
    try {
      const result = await api.sendVoice(
        conversationId,
        new Blob(chunks.current, { type: mimeType }),
      );
      setTranscript(result.userTranscript);
      setWarning(result.warning ?? "");
      onConversationUpdated(result.conversation);
      await playReturnedAudio(result);
    } catch (error) {
      onError((error as Error).message);
    } finally {
      setSending(false);
    }
  }

  async function playReturnedAudio(result: VoiceTurn) {
    if (!result.assistantAudioBase64 || !result.audioContentType) {
      return;
    }
    const binary = Uint8Array.from(atob(result.assistantAudioBase64), (value) =>
      value.charCodeAt(0),
    );
    const url = URL.createObjectURL(
      new Blob([binary], { type: result.audioContentType }),
    );
    setAudioUrl(url);
    try {
      await new Audio(url).play();
    } catch {
      setWarning("自動再生できませんでした。下の再生ボタンを押してください。");
    }
  }

  return (
    <section className="voice-recorder" aria-label="音声入力">
      <div className="voice-actions">
        {!recording ? (
          <button
            type="button"
            className="secondary microphone-button"
            disabled={disabled || sending}
            onClick={startRecording}
          >
            {sending ? "音声を処理中…" : "🎙️ 音声で話す"}
          </button>
        ) : (
          <button
            type="button"
            className="recording-button"
            onClick={stopRecording}
          >
            ■ 録音を停止（{seconds}秒）
          </button>
        )}
        {audioUrl && (
          <audio className="voice-player" src={audioUrl} controls>
            音声を再生できません。
          </audio>
        )}
      </div>
      <p className="voice-hint">
        最大60秒。停止後に文字起こしとAI応答を表示します。
      </p>
      {transcript && (
        <p className="transcript" aria-live="polite">
          <strong>認識した英語：</strong> {transcript}
        </p>
      )}
      {warning && (
        <p className="voice-warning" role="status">
          {warning}
        </p>
      )}
    </section>
  );
}
