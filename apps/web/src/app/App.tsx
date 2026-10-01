/** アプリ全体の認証状態とルーティングだけを構成し、画面固有の処理は各featureへ委譲します。 */

import { useEffect, useState } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { AuthForm } from "../features/auth/AuthForm";
import { ConversationPage } from "../features/conversation/ConversationPage";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { HistoryPage } from "../features/history/HistoryPage";
import { EnglishLevelPage } from "../features/onboarding/EnglishLevelPage";
import { api, User } from "../shared/api";
import { Shell } from "../shared/ui/Shell";

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
            <HistoryPage user={user} onLogout={logout} />
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
