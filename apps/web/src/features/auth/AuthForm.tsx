/** 登録とログインのフォーム状態・送信イベントを管理します。 */

import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, ApiError, User } from "../../shared/api";
import { ErrorBox } from "../../shared/ui/ErrorBox";

type AuthFormProps = {
  mode: "login" | "register";
  onDone: (user: User) => void;
};

export function AuthForm({ mode, onDone }: AuthFormProps) {
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    const data = new FormData(event.currentTarget);
    try {
      if (mode === "register") {
        await api.register({
          displayName: String(data.get("displayName")),
          email: String(data.get("email")),
          password: String(data.get("password")),
        });
        navigate("/login");
      } else {
        onDone(
          await api.login({
            email: String(data.get("email")),
            password: String(data.get("password")),
          }),
        );
      }
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "処理に失敗しました。",
      );
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
