/** 会話詳細の読込、送信、終了、Feedback更新を管理するfeature画面です。 */

import { FormEvent, KeyboardEvent, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { api, Conversation, User } from "../../shared/api";
import { ErrorBox } from "../../shared/ui/ErrorBox";
import { Shell } from "../../shared/ui/Shell";
import { StatusBadge } from "../../shared/ui/StatusBadge";
import { FeedbackPanel } from "./FeedbackPanel";
import { MessageList } from "./MessageList";
import { VoiceRecorder } from "./VoiceRecorder";

type ConversationPageProps = { user: User; onLogout: () => void };

export function ConversationPage({ user, onLogout }: ConversationPageProps) {
  const { id } = useParams();
  const [conversation, setConversation] = useState<Conversation>();
  const [text, setText] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [loadState, setLoadState] = useState<"LOADING" | "SUCCESS" | "ERROR">(
    "LOADING",
  );

  useEffect(() => {
    setLoadState("LOADING");
    api
      .detail(id!)
      .then((loadedConversation) => {
        setConversation(loadedConversation);
        setLoadState("SUCCESS");
      })
      .catch((requestError: Error) => {
        setError(requestError.message);
        setLoadState("ERROR");
      });
  }, [id]);

  useEffect(() => {
    if (conversation?.feedback?.status !== "GENERATING") {
      return;
    }
    const timer = window.setInterval(() => {
      api
        .detail(conversation.id)
        .then(setConversation)
        .catch((pollError: Error) => setError(pollError.message));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [conversation?.id, conversation?.feedback?.status]);

  async function send(event: FormEvent) {
    event.preventDefault();
    if (!conversation || !text.trim() || busy) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      setConversation(await api.send(conversation.id, text));
      setText("");
    } catch (requestError) {
      setError((requestError as Error).message);
    } finally {
      setBusy(false);
    }
  }

  function sendOnEnter(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (
      event.key !== "Enter" ||
      event.shiftKey ||
      event.nativeEvent.isComposing
    ) {
      return;
    }
    event.preventDefault();
    void send(event as unknown as FormEvent);
  }

  async function finish() {
    if (!conversation) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      setConversation(await api.finish(conversation.id));
    } catch (requestError) {
      setError((requestError as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function retryFeedback() {
    if (!conversation) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      setConversation(await api.retryFeedback(conversation.id));
    } catch (requestError) {
      setError((requestError as Error).message);
    } finally {
      setBusy(false);
    }
  }

  function updateMessage(updatedMessage: Conversation["messages"][number]) {
    setConversation((current) =>
      current
        ? {
            ...current,
            messages: current.messages.map((message) =>
              message.id === updatedMessage.id ? updatedMessage : message,
            ),
          }
        : current,
    );
  }

  return (
    <Shell user={user} onLogout={onLogout}>
      <div className="conversation-layout">
        <div className="conversation-head">
          <div>
            <p className="eyebrow">FREE CONVERSATION</p>
            <h1>
              {conversation?.status === "ENDED"
                ? "会話を振り返る"
                : "英会話セッション"}
            </h1>
          </div>
          {conversation?.status === "ACTIVE" && (
            <div className="conversation-head-actions">
              <ConversationUsageBadge conversation={conversation} />
              <button className="secondary" onClick={finish} disabled={busy}>
                会話を終了
              </button>
            </div>
          )}
          {conversation?.status === "ENDED" && (
            <StatusBadge status={conversation.status} />
          )}
        </div>
        {loadState === "LOADING" && (
          <p aria-live="polite">会話を読み込んでいます…</p>
        )}
        {loadState === "ERROR" && !conversation && (
          <p>
            会話を表示できません。ダッシュボードへ戻って、もう一度お試しください。
          </p>
        )}
        {conversation && (
          <MessageList
            conversation={conversation}
            onMessageUpdated={updateMessage}
            onError={setError}
          />
        )}
        <ErrorBox error={error} />
        {conversation?.status === "ACTIVE" && (
          <>
            <VoiceRecorder
              conversationId={conversation.id}
              disabled={busy}
              onConversationUpdated={setConversation}
              onError={setError}
            />
            <form className="composer" onSubmit={send}>
              <textarea
                aria-label="メッセージ"
                value={text}
                onChange={(event) => setText(event.target.value)}
                onKeyDown={sendOnEnter}
                maxLength={2000}
                rows={2}
                placeholder="Type your message in English…"
              />
              <button disabled={busy || !text.trim()}>
                {busy ? "送信中…" : "送信"}
              </button>
            </form>
            <div className="conversation-bottom-actions">
              <p className="composer-hint">
                Enter で送信 / Shift + Enter で改行
              </p>
              <button
                type="button"
                className="link finish-conversation-bottom"
                onClick={finish}
                disabled={busy}
              >
                会話を終了
              </button>
            </div>
          </>
        )}
        {conversation?.status === "ENDED" && (
          <FeedbackPanel
            conversation={conversation}
            retrying={busy}
            onRetry={retryFeedback}
          />
        )}
      </div>
    </Shell>
  );
}

function ConversationUsageBadge({
  conversation,
}: {
  conversation: Conversation;
}) {
  const tokens =
    conversation.llmUsage.inputTokens + conversation.llmUsage.outputTokens;
  const estimatedYen = conversation.llmUsage.estimatedCostMicros / 1_000_000;
  return (
    <div
      className="conversation-usage-badge"
      aria-label={`現在のAI利用量 ${tokens.toLocaleString()}トークン、概算${estimatedYen.toFixed(4)}円`}
    >
      <span>{tokens.toLocaleString()} tokens</span>
      <strong>約 {estimatedYen.toFixed(4)} 円</strong>
    </div>
  );
}
