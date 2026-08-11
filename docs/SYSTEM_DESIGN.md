<!-- TalkOn MVPフェーズ2時点の実装を、画面・API・状態・DB・AI・運用の観点から俯瞰する設計書です。 -->

# TalkOn システム設計書

## 1. 文書情報

| 項目           | 内容                                                      |
| -------------- | --------------------------------------------------------- |
| 対象           | TalkOn MVPフェーズ2                                       |
| 基準ブランチ   | `codex/mvp-phase2-conversation-quality`                   |
| フロントエンド | React 19 / TypeScript / Vite                              |
| バックエンド   | Java 21 / Spring Boot / Spring Security / Spring Data JPA |
| データベース   | H2（MySQL互換モード）/ Flyway                             |
| AI             | OpenAI互換Chat Completions API、または開発用Local AI      |
| 認証           | HTTP Session Cookie + CSRF                                |
| 実装状態       | Phase 2実装完了                                           |
| 最終同期日     | 2026-08-11                                                |

本書は現在のコードを正とし、MVPの構造、責務、状態遷移、主要な非機能方針を説明する。フェーズ移行の背景と残課題は `docs/MVPフェーズ2移行書.md` を参照する。

## 2. システム構成

```text
Browser
  └─ React / React Router
       └─ shared/api.ts
            │ HTTP JSON + JSESSIONID + X-XSRF-TOKEN
            ▼
Spring Boot
  ├─ Security / CSRF / CORS
  ├─ Controller
  ├─ Service / Transaction
  ├─ Repository / JPA
  ├─ H2 Database / Flyway
  └─ ConversationAiClient
       ├─ OpenAI互換API（OPENAI_API_KEYあり）
       └─ LocalAiClient（OPENAI_API_KEYなし）
```

フロントエンドとバックエンドは別Originで起動する。既定値は `http://localhost:5173` と `http://localhost:8080` である。ブラウザ通信はCORS、Session Cookie、CSRF Tokenの3条件を満たす必要がある。

## 3. 機能構成

| 機能           | フロントエンド                    | API入口                              | 業務処理                                      |
| -------------- | --------------------------------- | ------------------------------------ | --------------------------------------------- |
| 登録・ログイン | `App.tsx`                         | `AuthController`                     | `AuthenticationManager`、`UserRepository`     |
| 英語レベル選択 | `EnglishLevelPage.tsx`            | `UserProfileController`              | `User.selectEnglishLevel()`                   |
| ダッシュボード | `DashboardPage.tsx`               | `DashboardController`                | `DashboardService`                            |
| 会話開始・送信 | `ConversationPage`、`MessageList` | `ConversationController`             | `ConversationService`、`ConversationAiClient` |
| 翻訳           | `MessageList.tsx`                 | `ConversationController.translate()` | `ConversationService.translate()`             |
| 会話終了       | `ConversationPage`                | `ConversationController.finish()`    | `ConversationService.finish()`                |
| フィードバック | `FeedbackPanel.tsx`               | 詳細・再生成API                      | `FeedbackGenerationService`                   |
| 履歴           | `History`、`ConversationPage`     | 会話一覧・詳細API                    | `ConversationService.history/detail()`        |

## 4. 画面とルーティング

| URL                  | 認証 | 条件                   | 画面                         |
| -------------------- | ---- | ---------------------- | ---------------------------- |
| `/register`          | 不要 | 未ログイン             | アカウント登録               |
| `/login`             | 不要 | 未ログイン             | ログイン                     |
| `/onboarding`        | 必要 | `englishLevel == null` | 英語レベル選択               |
| `/`                  | 必要 | レベル選択済み         | ダッシュボード               |
| `/conversations/:id` | 必要 | 所有会話               | 進行中の会話                 |
| `/history`           | 必要 | レベル選択済み         | 会話履歴一覧                 |
| `/history/:id`       | 必要 | 所有会話               | 終了済み会話とフィードバック |

`App.tsx` が認証状態とルーティングを管理する。初期表示では `/api/auth/me` を呼び、未認証ならログイン、レベル未選択ならオンボーディングへ誘導する。

## 5. API設計

| Method | Path                                                       | 成功      | 説明                            |
| ------ | ---------------------------------------------------------- | --------- | ------------------------------- |
| GET    | `/api/csrf`                                                | 200       | CSRF Token取得                  |
| POST   | `/api/auth/register`                                       | 201       | ユーザー登録                    |
| POST   | `/api/auth/login`                                          | 200       | Session作成                     |
| POST   | `/api/auth/logout`                                         | 204       | Session破棄                     |
| GET    | `/api/auth/me`                                             | 200       | ログインユーザー取得            |
| PUT    | `/api/users/me/english-level`                              | 200       | 英語レベル保存                  |
| GET    | `/api/dashboard`                                           | 200       | 学習状況取得                    |
| POST   | `/api/conversations`                                       | 201 / 200 | 新規会話開始 / 進行中会話再利用 |
| GET    | `/api/conversations/active`                                | 200 / 204 | 進行中会話取得                  |
| GET    | `/api/conversations/{id}`                                  | 200       | 会話詳細取得                    |
| POST   | `/api/conversations/{id}/messages`                         | 200       | USER発言保存とAI返信生成        |
| POST   | `/api/conversations/{id}/finish`                           | 202       | 会話終了と非同期Feedback開始    |
| POST   | `/api/conversations/{id}/feedback/retry`                   | 202       | 失敗Feedback再生成              |
| POST   | `/api/conversations/{id}/messages/{messageId}/translation` | 200       | AI発言の翻訳取得・保存          |
| GET    | `/api/conversations?page=0&size=20`                        | 200       | 会話履歴取得                    |

エラーは `ApiError` 形式へ統一する。401ではフロントエンドがログイン画面へ遷移する。会話の所有者違反は、存在を推測させないため404を返す。

## 6. 主要処理

### 6.1 登録から会話開始まで

```text
登録
  → ログイン
  → Session Cookie発行
  → 英語レベル保存
  → Dashboard
  → 会話開始
  → AI挨拶をMessage(sequenceNo=1)として保存
```

ユーザーごとにACTIVEな会話は一つだけとする。開始API実行時にACTIVE会話があれば新規作成せず既存会話を返す。

### 6.2 メッセージ送信

```text
入力検証
  → 所有者・ACTIVE状態をロックして確認
  → USER Messageを保存
  → 保存済みの直近履歴とEnglishLevelをAIへ渡す
  → ASSISTANT Messageを保存
  → 会話詳細を返す
```

入力は1〜2,000文字、USER発言は1会話50件までである。AI呼び出し失敗時は503を返す。USER発言保存後にAIを呼ぶため、失敗時にもUSER発言は履歴へ残る。

### 6.3 会話終了とフィードバック

```text
POST finish
  → USER発言が1件以上あることを確認
  → Session = ENDED
  → Feedback = GENERATING
  → transaction commit
  → HTTP 202
  → @AsyncでFeedback生成
       ├─ 成功: COMPLETED
       └─ 2回失敗: FAILED
```

画面は `GENERATING` の間、1秒間隔で会話詳細を取得する。`FAILED` では再生成ボタンを表示する。会話終了自体はAI処理の成功・失敗に依存しない。

### 6.4 翻訳

ASSISTANT Messageだけを翻訳できる。初回だけAIへ問い合わせ、翻訳本文をMessageへ保存する。2回目以降は保存済み本文を返す。画面上の開閉状態はブラウザ内だけで管理する。

## 7. AI設計

`ConversationAiClient` をPortとし、Serviceから外部AIの詳細を分離する。

| メソッド    | 入力                   | 出力           |
| ----------- | ---------------------- | -------------- |
| `greeting`  | EnglishLevel           | 最初のAI挨拶   |
| `reply`     | 会話履歴、EnglishLevel | 会話返信       |
| `translate` | AI英文                 | 日本語訳       |
| `feedback`  | 全会話メッセージ       | `FeedbackData` |

外部AIには会話時に直近20メッセージを渡す。人格・会話方針・Feedback評価基準は `Prompts.java`、レベル差は `levelPolicy()` に集約する。FeedbackはBean Validationを通過したものだけ保存し、同文の無意味な訂正は `FeedbackData` で除外する。

Local AIはAPIキーなしで正常系を再現するための決定的実装であり、品質評価用の本番AIではない。

## 8. データ設計

| Table                    | 役割               | 主な列                                                               |
| ------------------------ | ------------------ | -------------------------------------------------------------------- |
| `users`                  | 認証・プロフィール | email、password_hash、english_level                                  |
| `conversation_sessions`  | 会話単位の状態     | user_id、status、started_at、finished_at                             |
| `conversation_messages`  | 発言履歴           | session_id、role、content、translation、sequence_no                  |
| `conversation_feedbacks` | 非同期Feedback     | session_id、status、summary、strengths、corrections、vocabulary_tips |

主要関係は `users 1:N conversation_sessions`、`conversation_sessions 1:N conversation_messages`、`conversation_sessions 1:1 conversation_feedbacks` である。

状態値：

- `EnglishLevel`: `BEGINNER` / `INTERMEDIATE` / `ADVANCED`
- `ConversationStatus`: `ACTIVE` / `ENDED`
- `MessageRole`: `USER` / `ASSISTANT`
- `FeedbackStatus`: `GENERATING` / `COMPLETED` / `FAILED`

適用済みMigrationは編集しない。変更時は `V4__...sql` のように新しいMigrationを追加する。

## 9. セキュリティ設計

- PasswordはBCrypt Hashだけを保存する
- 認証状態はHTTP Sessionへ保存する
- GET/HEAD以外はCSRF Tokenを要求する
- CookieはHttpOnly、SameSite=Lax。HTTPS環境では `COOKIE_SECURE=true` とする
- CORSは `CORS_ORIGIN` の単一Originと必要なHTTP Method/Headerだけを許可する
- 認証不要APIはCSRF取得、登録、ログイン、OpenAPI UIに限定する
- 会話操作では認証ユーザーIDと会話所有者を必ず照合する

## 10. ログ・障害処理

`RequestLoggingFilter` がrequestId、Method、Path、Status、処理時間を記録する。会話送信、終了、AI呼び出し、Feedback生成も処理時間と結果を記録する。会話本文やPasswordなどの機密情報はログへ出さない。

AI障害は会話中なら503、会話終了後のFeedbackなら `FAILED` 状態として扱う。予期しないフロントエンド例外は `ErrorBoundary` が白画面を避け、再読込導線を表示する。

## 11. テスト戦略

| 層                 | ツール                                    | 目的                             |
| ------------------ | ----------------------------------------- | -------------------------------- |
| Backend単体        | JUnit / AssertJ                           | DTO制約、Local AIロジック        |
| Backend統合        | SpringBootTest / MockMvc / H2             | 認証、認可、API、DB、非同期状態  |
| Frontend Component | Vitest / Testing Library                  | 画面分岐、操作、API呼び出し      |
| E2E                | Playwright                                | 登録からFeedbackまでの主要正常系 |
| 静的検査           | TypeScript / ESLint / Spotless / Prettier | 型・規約・フォーマット           |

詳細なケース、手順、期待結果、実施記録はGoogle Sheets向けの `TalkOnテスト仕様書.xlsx` で管理する。

Phase 2の完了判定では、Backend、Frontend、型検査、Lint、Format、E2Eを実行し、結果をテスト仕様書へ記録する。AI品質のうち外部モデル依存のケースとモバイル実機確認は、モデル名・Browser・証跡を残す手動試験として継続する。

## 12. 既知の制約

- Local AIは限定的な話題・誤りパターンだけに対応する
- AI品質を継続比較する評価データセットは未整備
- H2を使うMVP構成であり、本番RDB移行時の互換性検証が必要
- FeedbackのJSON Schema強制はプロンプトとJava Validation中心である
- 古い会話履歴は20メッセージを超えると会話AI入力から外れる
