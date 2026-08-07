/**
 * 認証・ホーム・会話・履歴画面とルーティングを構成します。MVPの画面フローを一つの入口で把握できるようにしています。
 */

import { FormEvent, useEffect, useState } from "react";
import {
  Link,
  Navigate,
  Route,
  Routes,
  useNavigate,
  useParams,
} from "react-router-dom";
import { DashboardPage } from "./features/dashboard/DashboardPage";
import { FeedbackPanel } from "./features/conversation/FeedbackPanel";
import { MessageList } from "./features/conversation/MessageList";
import { EnglishLevelPage } from "./features/onboarding/EnglishLevelPage";
import { api, ApiError, Conversation, HistoryPage, User } from "./shared/api";

function ErrorBox({ error }: { error: string }) {
  return error ? (
    <p className="error" role="alert">
      {error}
    </p>
  ) : null;
}

function Shell({
  children,
  user,
  onLogout,
}: {
  children: React.ReactNode;
  user: User;
  onLogout: () => void;
}) {
  return (
    <>
      <header>
        <Link className="brand" to="/">
          TalkOn
        </Link>
        <nav>
          <Link to="/history">履歴</Link>
          <span>{user.displayName}</span>
          <button className="link" onClick={onLogout}>
            ログアウト
          </button>
        </nav>
      </header>
      <main>{children}</main>
    </>
  );
}

function AuthForm({
  mode,
  onDone,
}: {
  mode: "login" | "register";
  onDone: (u: User) => void;
}) {
  const nav = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError("");
    const data = new FormData(e.currentTarget);
    try {
      if (mode === "register") {
        await api.register({
          displayName: String(data.get("displayName")),
          email: String(data.get("email")),
          password: String(data.get("password")),
        });
        nav("/login");
      } else {
        onDone(
          await api.login({
            email: String(data.get("email")),
            password: String(data.get("password")),
          }),
        );
      }
    } catch (x) {
      setError(x instanceof ApiError ? x.message : "処理に失敗しました。");
    } finally {
      setBusy(false);
    }
  }
  return (
    <main className="auth">
      <section className="card">
        <div className="logo">TalkOn</div>
        <h1>{mode === "login" ? "おかえりなさい" : "アカウント作成"}</h1>
        <p className="muted">AIとの会話で、英語をもっと身近に。</p>
        <form onSubmit={submit}>
          {mode === "register" && (
            <label>
              表示名
              <input name="displayName" required minLength={1} maxLength={50} />
            </label>
          )}
          <label>
            メールアドレス
            <input name="email" type="email" required maxLength={255} />
          </label>
          <label>
            パスワード
            <input
              name="password"
              type="password"
              required
              minLength={8}
              maxLength={72}
            />
          </label>
          <ErrorBox error={error} />
          <button disabled={busy}>
            {busy ? "処理中…" : mode === "login" ? "ログイン" : "登録する"}
          </button>
        </form>
        <p>
          {mode === "login" ? (
            <>
              初めてですか？ <Link to="/register">登録</Link>
            </>
          ) : (
            <>
              登録済みですか？ <Link to="/login">ログイン</Link>
            </>
          )}
        </p>
      </section>
    </main>
  );
}

function ConversationPage({
  user,
  onLogout,
}: {
  user: User;
  onLogout: () => void;
}) {
  const { id } = useParams();
  const [c, setC] = useState<Conversation>();
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
      .then((conversation) => {
        setC(conversation);
        setLoadState("SUCCESS");
      })
      .catch((e) => {
        setError(e.message);
        setLoadState("ERROR");
      });
  }, [id]);
  useEffect(() => {
    if (c?.feedback?.status !== "GENERATING") {
      return;
    }
    const timer = window.setInterval(() => {
      api
        .detail(c.id)
        .then(setC)
        .catch((pollError: Error) => setError(pollError.message));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [c?.id, c?.feedback?.status]);
  async function send(e: FormEvent) {
    e.preventDefault();
    if (!c || !text.trim()) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      setC(await api.send(c.id, text));
      setText("");
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function finish() {
    if (!c) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      setC(await api.finish(c.id));
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function retryFeedback() {
    if (!c) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      setC(await api.retryFeedback(c.id));
    } catch (retryError) {
      setError((retryError as Error).message);
    } finally {
      setBusy(false);
    }
  }
  function updateMessage(updatedMessage: Conversation["messages"][number]) {
    setC((current) =>
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
              {c?.status === "ENDED" ? "会話を振り返る" : "英会話セッション"}
            </h1>
          </div>
          {c?.status === "ACTIVE" && (
            <button className="secondary" onClick={finish} disabled={busy}>
              会話を終了
            </button>
          )}
        </div>
        {loadState === "LOADING" && (
          <p aria-live="polite">会話を読み込んでいます…</p>
        )}
        {loadState === "ERROR" && !c && (
          <p>
            会話を表示できません。ダッシュボードへ戻って、もう一度お試しください。
          </p>
        )}
        {c && (
          <MessageList
            conversation={c}
            onMessageUpdated={updateMessage}
            onError={setError}
          />
        )}
        <ErrorBox error={error} />
        {c?.status === "ACTIVE" && (
          <form className="composer" onSubmit={send}>
            <textarea
              aria-label="メッセージ"
              value={text}
              onChange={(e) => setText(e.target.value)}
              maxLength={2000}
              placeholder="Type your message in English…"
            />
            <button disabled={busy || !text.trim()}>
              {busy ? "送信中…" : "送信"}
            </button>
          </form>
        )}
        {c?.status === "ENDED" && (
          <FeedbackPanel
            conversation={c}
            retrying={busy}
            onRetry={retryFeedback}
          />
        )}
      </div>
    </Shell>
  );
}

function History({ user, onLogout }: { user: User; onLogout: () => void }) {
  const [data, setData] = useState<HistoryPage>();
  const [error, setError] = useState("");
  useEffect(() => {
    api
      .history()
      .then(setData)
      .catch((e) => setError(e.message));
  }, []);
  return (
    <Shell user={user} onLogout={onLogout}>
      <p className="eyebrow">YOUR PROGRESS</p>
      <h1>会話履歴</h1>
      <ErrorBox error={error} />
      <div className="history">
        {data?.content.map((c) => (
          <Link key={c.id} to={`/history/${c.id}`}>
            <div>
              <strong>{new Date(c.startedAt).toLocaleString()}</strong>
              <span>{c.status}</span>
            </div>
            <span>詳細を見る →</span>
          </Link>
        ))}
        {data?.content.length === 0 && <p>まだ会話履歴がありません。</p>}
      </div>
    </Shell>
  );
}

export default function App() {
  const [user, setUser] = useState<User | null | undefined>(undefined);
  useEffect(() => {
    api
      .me()
      .then(setUser)
      .catch(() => setUser(null));
  }, []);
  if (user === undefined) {
    return <main className="loading">TalkOn</main>;
  }
  if (user && !user.englishLevel) {
    return (
      <Routes>
        <Route
          path="/onboarding"
          element={<EnglishLevelPage onSelected={setUser} />}
        />
        <Route path="*" element={<Navigate to="/onboarding" />} />
      </Routes>
    );
  }
  async function logout() {
    await api.logout();
    setUser(null);
  }
  return (
    <Routes>
      <Route
        path="/login"
        element={
          user ? (
            <Navigate to="/" />
          ) : (
            <AuthForm mode="login" onDone={setUser} />
          )
        }
      />
      <Route
        path="/register"
        element={
          user ? (
            <Navigate to="/" />
          ) : (
            <AuthForm mode="register" onDone={setUser} />
          )
        }
      />
      <Route
        path="/"
        element={
          user ? (
            <Shell user={user} onLogout={logout}>
              <DashboardPage displayName={user.displayName} />
            </Shell>
          ) : (
            <Navigate to="/login" />
          )
        }
      />
      <Route
        path="/conversations/:id"
        element={
          user ? (
            <ConversationPage user={user} onLogout={logout} />
          ) : (
            <Navigate to="/login" />
          )
        }
      />
      <Route
        path="/history"
        element={
          user ? (
            <History user={user} onLogout={logout} />
          ) : (
            <Navigate to="/login" />
          )
        }
      />
      <Route
        path="/history/:id"
        element={
          user ? (
            <ConversationPage user={user} onLogout={logout} />
          ) : (
            <Navigate to="/login" />
          )
        }
      />
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  );
}
