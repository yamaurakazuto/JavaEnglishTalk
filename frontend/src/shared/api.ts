/**
 * 認証Cookie・CSRF・共通エラーを扱うAPIクライアントです。画面ごとの通信処理の重複を避けるために集約しています。
 */

export type EnglishLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
export type User = {
  id: number;
  displayName: string;
  email: string;
  englishLevel: EnglishLevel | null;
};
export type Message = {
  id: number;
  role: "USER" | "ASSISTANT";
  content: string;
  translation: string | null;
  sequenceNo: number;
  createdAt: string;
};
export type Feedback = {
  status: "GENERATING" | "COMPLETED" | "FAILED";
  summary: string | null;
  strengths: string[];
  corrections: {
    original: string;
    corrected: string;
    reasonJa: string;
    alternative: string;
    category: "GRAMMAR" | "VOCABULARY" | "NATURALNESS" | "WORD_ORDER";
  }[];
  vocabularyTips: string[];
  overallComment: string | null;
  errorMessage: string | null;
};
export type Conversation = {
  id: number;
  status: "ACTIVE" | "ENDED";
  startedAt: string;
  finishedAt: string | null;
  messages: Message[];
  feedback: Feedback | null;
  llmUsage: {
    inputTokens: number;
    outputTokens: number;
    estimatedCostMicros: number;
    model: string | null;
  };
};

export type VoiceTurn = {
  userTranscript: string;
  conversation: Conversation;
  assistantAudioBase64: string | null;
  audioContentType: string | null;
  processingTimes: {
    sttMs: number;
    llmMs: number;
    ttsMs: number;
    totalMs: number;
  };
  warning: string | null;
};
export type HistoryPage = {
  content: Pick<Conversation, "id" | "status" | "startedAt" | "finishedAt">[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type DailyActivity = {
  date: string;
  sessionCount: number;
  studySeconds: number;
  level: number;
  messageCount: number;
};

export type DashboardData = {
  todayStudySeconds: number;
  currentStreakDays: number;
  totalStudyDays: number;
  activeConversationId: number | null;
  activities: DailyActivity[];
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
  if (init.body && !(init.body instanceof FormData)) {
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

async function requestAudio(path: string): Promise<Blob> {
  if (!csrf) {
    const response = await fetch(`${base}/api/csrf`, {
      credentials: "include",
    });
    csrf = (await response.json()).token;
  }
  const response = await fetch(`${base}${path}`, {
    method: "POST",
    credentials: "include",
    headers: { "X-XSRF-TOKEN": csrf! },
  });
  if (!response.ok) {
    const body = await response
      .json()
      .catch(() => ({ message: "音声を生成できませんでした。" }));
    throw new ApiError(response.status, body.message);
  }
  return response.blob();
}

export const api = {
  me: () => request<User>("/api/auth/me"),
  dashboard: () => request<DashboardData>("/api/dashboard"),
  register: async (x: {
    displayName: string;
    email: string;
    password: string;
  }) => {
    const user = await request<User>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(x),
    });
    csrf = undefined;

    return user;
  },
  login: async (x: { email: string; password: string }) => {
    const user = await request<User>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(x),
    });
    csrf = undefined;

    return user;
  },
  logout: async () => {
    await request<void>("/api/auth/logout", { method: "POST" });
    csrf = undefined;
  },
  selectEnglishLevel: (englishLevel: EnglishLevel) =>
    request<User>("/api/users/me/english-level", {
      method: "PUT",
      body: JSON.stringify({ englishLevel }),
    }),
  start: () => request<Conversation>("/api/conversations", { method: "POST" }),
  active: () => request<Conversation | undefined>("/api/conversations/active"),
  detail: (id: string | number) =>
    request<Conversation>(`/api/conversations/${id}`),
  send: (id: number, content: string) =>
    request<Conversation>(`/api/conversations/${id}/messages`, {
      method: "POST",
      body: JSON.stringify({ content }),
    }),
  sendVoice: (id: number, audio: Blob) => {
    const body = new FormData();
    body.append(
      "audio",
      audio,
      `recording.${audio.type.includes("webm") ? "webm" : "mp4"}`,
    );
    return request<VoiceTurn>(`/api/conversations/${id}/voice-turns`, {
      method: "POST",
      body,
    });
  },
  speech: (conversationId: number, messageId: number) =>
    requestAudio(
      `/api/conversations/${conversationId}/messages/${messageId}/speech`,
    ),
  finish: (id: number) =>
    request<Conversation>(`/api/conversations/${id}/finish`, {
      method: "POST",
    }),
  retryFeedback: (id: number) =>
    request<Conversation>(`/api/conversations/${id}/feedback/retry`, {
      method: "POST",
    }),
  translate: (conversationId: number, messageId: number) =>
    request<Message>(
      `/api/conversations/${conversationId}/messages/${messageId}/translation`,
      { method: "POST" },
    ),
  history: () => request<HistoryPage>("/api/conversations?page=0&size=20"),
};
