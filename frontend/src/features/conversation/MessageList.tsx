/**
 * 会話メッセージと任意の日本語訳を表示します。英語を先に読み、必要なときだけ補助を開ける構成です。
 */

import { useState } from "react";
import { api, Conversation, Message } from "../../shared/api";

type MessageListProps = {
  conversation: Conversation;
  onMessageUpdated: (message: Message) => void;
  onError: (message: string) => void;
};

export function MessageList({
  conversation,
  onMessageUpdated,
  onError,
}: MessageListProps) {
  const [translatingId, setTranslatingId] = useState<number>();
  const [speakingId, setSpeakingId] = useState<number>();
  const [openTranslations, setOpenTranslations] = useState<number[]>([]);

  async function showTranslation(message: Message) {
    if (message.translation) {
      toggleTranslation(message.id);
      return;
    }
    setTranslatingId(message.id);
    onError("");
    try {
      onMessageUpdated(await api.translate(conversation.id, message.id));
      setOpenTranslations((current) => [...current, message.id]);
    } catch (error) {
      onError((error as Error).message);
    } finally {
      setTranslatingId(undefined);
    }
  }

  function toggleTranslation(messageId: number) {
    setOpenTranslations((current) =>
      current.includes(messageId)
        ? current.filter((id) => id !== messageId)
        : [...current, messageId],
    );
  }

  async function playSpeech(message: Message) {
    setSpeakingId(message.id);
    onError("");
    try {
      const audio = await api.speech(conversation.id, message.id);
      const url = URL.createObjectURL(audio);
      const player = new Audio(url);
      player.addEventListener("ended", () => URL.revokeObjectURL(url), {
        once: true,
      });
      await player.play();
    } catch (error) {
      onError((error as Error).message);
    } finally {
      setSpeakingId(undefined);
    }
  }

  return (
    <div className="messages">
      {conversation.messages.map((message) => (
        <div
          key={message.id}
          className={`message ${message.role.toLowerCase()}`}
        >
          <small>{message.role === "USER" ? "You" : "TalkOn"}</small>
          <p>{message.content}</p>
          {message.role === "ASSISTANT" && (
            <>
              {message.translation && openTranslations.includes(message.id) && (
                <p className="translation" lang="ja">
                  {message.translation}
                </p>
              )}
              <button
                className="translation-button"
                type="button"
                disabled={translatingId === message.id}
                onClick={() => showTranslation(message)}
              >
                {translatingId === message.id
                  ? "翻訳しています…"
                  : message.translation && openTranslations.includes(message.id)
                    ? "🌐 日本語訳を閉じる"
                    : "🌐 日本語訳を見る"}
              </button>
              <button
                className="speech-button"
                type="button"
                disabled={speakingId === message.id}
                onClick={() => playSpeech(message)}
              >
                {speakingId === message.id ? "音声を準備中…" : "🔊 英語を聞く"}
              </button>
            </>
          )}
        </div>
      ))}
    </div>
  );
}
