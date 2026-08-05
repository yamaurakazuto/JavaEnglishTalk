/**
 * 認証Cookie・CSRF・共通エラーを扱うAPIクライアントです。画面ごとの通信処理の重複を避けるために集約しています。
 */

export type User = { id: number; displayName: string; email: string };
export type Message = {
  id: number;
  role: "USER" | "ASSISTANT";
  content: string;
  sequenceNo: number;
  createdAt: string;
};
export type Feedback = {
  summary: string;
  strengths: string[];
  improvements: { original: string; reason: string; suggestion: string }[];
  corrections: { original: string; corrected: string; explanation: string }[];
  overallComment: string;
};
export type Conversation = {
  id: number;
  status: "ACTIVE" | "FEEDBACK_GENERATING" | "COMPLETED";
  startedAt: string;
  finishedAt: string | null;
  messages: Message[];
  feedback: Feedback | null;
};
export type HistoryPage = {
  content: Pick<Conversation, "id" | "status" | "startedAt" | "finishedAt">[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
  }
}
const base = import.meta.env.VITE_API_URL ?? "http://localhost:8080";
let csrf: string | undefined;
async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = init.method ?? "GET";
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (method !== "GET" && method !== "HEAD") {
    if (!csrf) {
      const r = await fetch(`${base}/api/csrf`, { credentials: "include" });
      csrf = (await r.json()).token;
    }
    headers.set("X-XSRF-TOKEN", csrf!);
  }
  if (init.body) {
    headers.set("Content-Type", "application/json");
  }
  const response = await fetch(`${base}${path}`, {
    ...init,
    headers,
    credentials: "include",
  });
  if (response.status === 204) {
    return undefined as T;
  }
  const body = await response
    .json()
    .catch(() => ({ message: "通信に失敗しました。" }));
  if (!response.ok) {
    if (
      response.status === 401 &&
      path !== "/api/auth/me" &&
      location.pathname !== "/login"
    ) {
      location.assign("/login");
    }
    throw new ApiError(response.status, body.message ?? "処理に失敗しました。");
  }
  return body as T;
}
export const api = {
  me: () => request<User>("/api/auth/me"),
  register: (x: { displayName: string; email: string; password: string }) =>
    request<User>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(x),
    }),
  login: (x: { email: string; password: string }) =>
    request<User>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(x),
    }),
  logout: () => request<void>("/api/auth/logout", { method: "POST" }),
  start: () => request<Conversation>("/api/conversations", { method: "POST" }),
  active: () => request<Conversation | undefined>("/api/conversations/active"),
  detail: (id: string | number) =>
    request<Conversation>(`/api/conversations/${id}`),
  send: (id: number, content: string) =>
    request<Conversation>(`/api/conversations/${id}/messages`, {
      method: "POST",
      body: JSON.stringify({ content }),
    }),
  finish: (id: number) =>
    request<Conversation>(`/api/conversations/${id}/finish`, {
      method: "POST",
    }),
  history: () => request<HistoryPage>("/api/conversations?page=0&size=20"),
};
