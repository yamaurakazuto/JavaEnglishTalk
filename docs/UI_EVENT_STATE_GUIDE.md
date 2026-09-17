<!-- TalkOnの画面状態とユーザーイベントについて、定義・更新・保存を担当するファイルを逆引きする手順書です。 -->

# 画面状態・イベント追跡手順書

## 1. 目的

この手順書は、画面に表示されている状態やユーザー操作を変更・調査するときに、どのファイルを確認すればよいかを示す。

TalkOnの状態は、次の3層に分かれている。

| 状態の種類         | 例                                                                    | 主な場所                                                  |
| ------------------ | --------------------------------------------------------------------- | --------------------------------------------------------- |
| 一時的な画面状態   | 読み込み中、送信中、エラー、翻訳の開閉、録音秒数                      | Reactコンポーネントの `useState`                          |
| APIで共有する状態  | `User`、`Conversation`、`Feedback`、`DashboardData`                   | `frontend/src/shared/api.ts` のTypeScript型、BackendのDTO |
| 永続化する業務状態 | 会話の `ACTIVE / ENDED`、Feedbackの `GENERATING / COMPLETED / FAILED` | BackendのEnum・Entity、Flyway Migration                   |

表示だけを変える場合でも、APIレスポンスや永続状態が条件になっていないかを確認する。基本の追跡順は次のとおり。

```text
画面コンポーネント
  → イベントハンドラー
  → frontend/src/shared/api.ts
  → Backend Controller
  → Service
  → Entity / Repository / DB
  → DTO
  → 画面の表示条件
```

## 2. 画面別の状態ファイル

| 画面・部品               | URL                  | 表示を構成するファイル                                       | 主な画面状態・表示条件                                                                             |
| ------------------------ | -------------------- | ------------------------------------------------------------ | -------------------------------------------------------------------------------------------------- |
| アプリ全体・ルーティング | 全URL                | `frontend/src/App.tsx`                                       | `user` が `undefined` なら初期読込、`null` なら未認証、`englishLevel` が未設定ならオンボーディング |
| ログイン                 | `/login`             | `frontend/src/App.tsx` の `AuthForm`                         | `mode="login"`、`busy`、`error`                                                                    |
| ユーザー登録             | `/register`          | `frontend/src/App.tsx` の `AuthForm`                         | `mode="register"`、`busy`、`error`                                                                 |
| オンボーディング         | `/onboarding`        | `frontend/src/features/onboarding/EnglishLevelPage.tsx`      | 選択肢は `levels`、処理中の選択肢は `busy`、失敗は `error`                                         |
| ダッシュボード           | `/`                  | `frontend/src/features/dashboard/DashboardPage.tsx`          | `dashboard`、`busy`、`error`、`activeConversationId`                                               |
| 学習サマリー             | `/` 内               | `frontend/src/features/dashboard/StudySummary.tsx`           | 親から受け取る学習時間・継続日数・総学習日数                                                       |
| アクティビティ           | `/` 内               | `frontend/src/features/dashboard/ActivityGrid.tsx`           | `activities` から週・日・強度を表示                                                                |
| 会話                     | `/conversations/:id` | `frontend/src/App.tsx` の `ConversationPage`                 | `c`、`text`、`busy`、`error`、`loadState`                                                          |
| 会話履歴詳細             | `/history/:id`       | `frontend/src/App.tsx` の `ConversationPage`                 | 会話画面と共通。`c.status` が `ENDED` なら振り返り表示                                             |
| メッセージ一覧           | 会話画面内           | `frontend/src/features/conversation/MessageList.tsx`         | `translatingId`、`speakingId`、`openTranslations`                                                  |
| 単語翻訳ツールチップ     | AIメッセージ内       | `frontend/src/features/conversation/WordTranslationText.tsx` | `tooltip`、翻訳キャッシュ、300msのhover/focus待機                                                  |
| 音声入力                 | 会話画面内           | `frontend/src/features/conversation/VoiceRecorder.tsx`       | `recording`、`sending`、`seconds`、`transcript`、`warning`、`audioUrl`                             |
| Feedback                 | 終了済み会話内       | `frontend/src/features/conversation/FeedbackPanel.tsx`       | `feedback.status` ごとに生成中・失敗・完了を分岐                                                   |
| 会話履歴一覧             | `/history`           | `frontend/src/App.tsx` の `History`                          | `data`、`error`、履歴0件表示                                                                       |
| 共通エラー境界           | React全体            | `frontend/src/ErrorBoundary.tsx`                             | 未捕捉の描画エラー                                                                                 |
| 見た目・レスポンシブ     | 全画面               | `frontend/src/styles.css`                                    | class名に対応する色、配置、表示幅                                                                  |

画面で利用するAPIレスポンス型と呼び出し関数は、すべて `frontend/src/shared/api.ts` に集約されている。

## 3. イベント別のファイル位置

### 3.1 認証・ユーザー設定

| イベント       | 画面イベント                      | Frontend API               | Backend入口                           | 状態更新・保存                                     |
| -------------- | --------------------------------- | -------------------------- | ------------------------------------- | -------------------------------------------------- |
| 初期認証確認   | `App` の初回 `useEffect`          | `api.me()`                 | `AuthController.me()`                 | HTTP SessionからUserを取得し、`App.user` を更新    |
| ユーザー登録   | `AuthForm.submit()`               | `api.register()`           | `AuthController.register()`           | `User` を保存。成功後 `/login` へ遷移              |
| ログイン       | `AuthForm.submit()`               | `api.login()`              | `AuthController.login()`              | HTTP Sessionを作成し、`App.user` を更新            |
| ログアウト     | `Shell` のボタン → `App.logout()` | `api.logout()`             | `AuthController.logout()`             | Sessionを破棄し、`App.user = null`                 |
| 英語レベル選択 | `EnglishLevelPage.select()`       | `api.selectEnglishLevel()` | `UserProfileController.selectLevel()` | `User.selectEnglishLevel()`、`users.english_level` |

関連ファイル:

- 認証・認可規則: `backend/src/main/java/com/talkon/auth/SecurityConfig.java`
- ログインユーザー表現: `backend/src/main/java/com/talkon/auth/TalkOnPrincipal.java`
- User Entity: `backend/src/main/java/com/talkon/user/User.java`
- レベル値: `backend/src/main/java/com/talkon/user/EnglishLevel.java`

### 3.2 ダッシュボード・履歴

| イベント           | 画面イベント                             | Frontend API      | Backend入口                        | 集計・取得                                              |
| ------------------ | ---------------------------------------- | ----------------- | ---------------------------------- | ------------------------------------------------------- |
| ダッシュボード表示 | `DashboardPage` の初回 `useEffect`       | `api.dashboard()` | `DashboardController.dashboard()`  | `DashboardService.getDashboard()`                       |
| 会話開始・再開     | `DashboardPage.openConversation()`       | `api.start()`     | `ConversationController.start()`   | `ConversationService.start()`。ACTIVE会話があれば再利用 |
| 履歴画面へ移動     | `navigate("/history")` またはHeader Link | なし              | なし                               | React Routerで画面遷移                                  |
| 履歴読込           | `History` の初回 `useEffect`             | `api.history()`   | `ConversationController.history()` | `ConversationService.history()`                         |
| 履歴詳細表示       | 履歴の `Link`                            | `api.detail(id)`  | `ConversationController.detail()`  | `ConversationService.detail()`                          |

ダッシュボードのレスポンス形は `backend/src/main/java/com/talkon/dashboard/DashboardResponse.java`、Frontend型は `DashboardData` と `DailyActivity` を確認する。

### 3.3 テキスト会話

| イベント     | 画面イベント                           | Frontend API     | Backend入口                       | 状態更新・保存                                              |
| ------------ | -------------------------------------- | ---------------- | --------------------------------- | ----------------------------------------------------------- |
| 会話詳細読込 | `ConversationPage` の `useEffect`      | `api.detail(id)` | `ConversationController.detail()` | `loadState: LOADING → SUCCESS / ERROR`                      |
| 入力変更     | `textarea.onChange`                    | なし             | なし                              | `ConversationPage.text`                                     |
| 送信ボタン   | `form.onSubmit` → `send()`             | `api.send()`     | `ConversationController.send()`   | USER/ASSISTANTメッセージ、会話のLLM利用量                   |
| Enter送信    | `textarea.onKeyDown` → `sendOnEnter()` | `api.send()`     | 同上                              | Shift+EnterとIME変換中は送信しない                          |
| 会話終了     | 上下の終了ボタン → `finish()`          | `api.finish()`   | `ConversationController.finish()` | `ConversationSession.end()`、Feedbackを `GENERATING` で作成 |

AI会話内容は `ConversationAIService.java`、実際のAIクライアントとFake実装は `backend/src/main/java/com/talkon/llm/AiClientConfig.java`、プロンプトは `backend/src/main/java/com/talkon/llm/Prompts.java` を確認する。

### 3.4 翻訳・読み上げ

| イベント             | 画面イベント                      | Frontend API          | Backend入口                              | 状態更新・保存                                                                    |
| -------------------- | --------------------------------- | --------------------- | ---------------------------------------- | --------------------------------------------------------------------------------- |
| 文全体の翻訳を開く   | `MessageList.showTranslation()`   | `api.translate()`     | `ConversationController.translate()`     | 初回だけ `ConversationMessage.translation` へ保存。開閉は `openTranslations` のみ |
| 文全体の翻訳を閉じる | `MessageList.toggleTranslation()` | なし                  | なし                                     | `openTranslations` からIDを除く                                                   |
| 単語訳表示           | `WordTranslationText.showWord()`  | `api.translateWord()` | `ConversationController.translateWord()` | コンポーネント内キャッシュのみ。DB保存なし                                        |
| 単語訳非表示         | mouse leave / blur → `hideWord()` | なし                  | なし                                     | `tooltip` をクリア                                                                |
| 英文読み上げ         | `MessageList.playSpeech()`        | `api.speech()`        | `VoiceConversationController.speech()`   | 音声Blobを一時再生。DB保存なし                                                    |

翻訳処理本体は `backend/src/main/java/com/talkon/conversation/TranslationService.java`、音声合成本体は `backend/src/main/java/com/talkon/speech/TextToSpeechService.java` を確認する。

### 3.5 音声会話

| イベント         | 画面イベント                             | Frontend API       | Backend入口                               | 状態更新・保存                                                 |
| ---------------- | ---------------------------------------- | ------------------ | ----------------------------------------- | -------------------------------------------------------------- |
| 録音開始         | `VoiceRecorder.startRecording()`         | なし               | なし                                      | マイク取得、`recording = true`、録音秒数を更新                 |
| 録音停止         | ボタンまたは60秒到達 → `stopRecording()` | なし               | なし                                      | `MediaRecorder.onstop` から送信へ進む                          |
| 音声送信         | `submitRecording()`                      | `api.sendVoice()`  | `VoiceConversationController.voiceTurn()` | `VoiceConversationService.send()` がSTT → 会話送信 → TTSを実行 |
| AI音声自動再生   | `playReturnedAudio()`                    | レスポンス内Base64 | なし                                      | `audioUrl` を作成。失敗時は `warning` を表示                   |
| 録音画面を離れる | `VoiceRecorder` のcleanup                | なし               | なし                                      | マイク、timer、Object URLを解放                                |

STTは `SpeechRecognitionService.java`、TTSは `TextToSpeechService.java`、OpenAIとの通信実装は `SpeechClientConfig.java`、サイズ等の設定は `SpeechProperties.java` と `application.yml` を確認する。

### 3.6 Feedback

| イベント     | 画面イベント                        | Frontend API          | Backend入口                              | 状態更新・保存                                  |
| ------------ | ----------------------------------- | --------------------- | ---------------------------------------- | ----------------------------------------------- |
| 会話終了直後 | `ConversationPage.finish()`         | `api.finish()`        | `ConversationController.finish()`        | 会話を `ENDED`、Feedbackを `GENERATING` にする  |
| 生成状態確認 | `ConversationPage` の1秒polling     | `api.detail()`        | `ConversationController.detail()`        | `GENERATING` 以外になるまで `c` を更新          |
| 生成成功     | 自動処理                            | なし                  | `FeedbackGenerationService.generate()`   | `ConversationFeedback.complete()` → `COMPLETED` |
| 生成失敗     | 自動処理                            | なし                  | `FeedbackGenerationService.generate()`   | `ConversationFeedback.fail()` → `FAILED`        |
| 再生成       | `FeedbackPanel` → `retryFeedback()` | `api.retryFeedback()` | `ConversationController.retryFeedback()` | `ConversationFeedback.retry()` → `GENERATING`   |

Feedbackの構造は `backend/src/main/java/com/talkon/feedback/FeedbackData.java`、カテゴリは `FeedbackCategory.java`、API変換は `ConversationDtos.feedback()` を確認する。

## 4. 永続状態と状態遷移

### 会話状態

```text
ConversationSession生成
  → ACTIVE
  → ConversationService.finish()
  → ENDED
```

| 確認内容             | ファイル                                                                 |
| -------------------- | ------------------------------------------------------------------------ |
| 許可される値         | `backend/src/main/java/com/talkon/conversation/ConversationStatus.java`  |
| 初期値・更新メソッド | `backend/src/main/java/com/talkon/conversation/ConversationSession.java` |
| 遷移条件・エラー     | `backend/src/main/java/com/talkon/conversation/ConversationService.java` |
| Frontend型           | `frontend/src/shared/api.ts` の `Conversation.status`                    |
| 表示分岐             | `frontend/src/App.tsx` の `ConversationPage`、`StatusBadge`              |
| DB列                 | `conversation_sessions.status`                                           |

### Feedback状態

```text
GENERATING
  ├─ complete() → COMPLETED
  └─ fail()     → FAILED
                     └─ retry() → GENERATING
```

| 確認内容             | ファイル                                                                   |
| -------------------- | -------------------------------------------------------------------------- |
| 許可される値         | `backend/src/main/java/com/talkon/feedback/FeedbackStatus.java`            |
| 初期値・更新メソッド | `backend/src/main/java/com/talkon/feedback/ConversationFeedback.java`      |
| 非同期生成           | `backend/src/main/java/com/talkon/feedback/FeedbackGenerationService.java` |
| Frontend型           | `frontend/src/shared/api.ts` の `Feedback.status`                          |
| 表示分岐             | `frontend/src/features/conversation/FeedbackPanel.tsx`                     |
| polling              | `frontend/src/App.tsx` の `ConversationPage`                               |
| DB列                 | `conversation_feedbacks.status`                                            |

### その他の永続データ

| データ                     | Entity                      | DBテーブル・列                      | 表示・利用箇所                                     |
| -------------------------- | --------------------------- | ----------------------------------- | -------------------------------------------------- |
| 英語レベル                 | `User.java`                 | `users.english_level`               | `App.tsx`、`EnglishLevelPage.tsx`、AIプロンプト    |
| メッセージ本文・役割・順序 | `ConversationMessage.java`  | `conversation_messages`             | `MessageList.tsx`                                  |
| 文全体の翻訳               | `ConversationMessage.java`  | `conversation_messages.translation` | `MessageList.tsx`                                  |
| Feedback内容               | `ConversationFeedback.java` | `conversation_feedbacks`            | `FeedbackPanel.tsx`                                |
| LLM利用量・料金            | `ConversationSession.java`  | `conversation_sessions.llm_*`       | `ConversationUsageBadge`、`FeedbackPanel.LlmUsage` |

DB定義は `backend/src/main/resources/db/migration/` のMigrationを番号順に確認する。適用済みMigrationは編集せず、スキーマ変更時は新しい番号のファイルを追加する。

## 5. API契約の対応表

| Method / Path                                                        | Frontend関数             | Controller / メソッド                   |
| -------------------------------------------------------------------- | ------------------------ | --------------------------------------- |
| `GET /api/auth/me`                                                   | `api.me`                 | `AuthController.me`                     |
| `POST /api/auth/register`                                            | `api.register`           | `AuthController.register`               |
| `POST /api/auth/login`                                               | `api.login`              | `AuthController.login`                  |
| `POST /api/auth/logout`                                              | `api.logout`             | `AuthController.logout`                 |
| `PUT /api/users/me/english-level`                                    | `api.selectEnglishLevel` | `UserProfileController.selectLevel`     |
| `GET /api/dashboard`                                                 | `api.dashboard`          | `DashboardController.dashboard`         |
| `POST /api/conversations`                                            | `api.start`              | `ConversationController.start`          |
| `GET /api/conversations/{id}`                                        | `api.detail`             | `ConversationController.detail`         |
| `GET /api/conversations`                                             | `api.history`            | `ConversationController.history`        |
| `POST /api/conversations/{id}/messages`                              | `api.send`               | `ConversationController.send`           |
| `POST /api/conversations/{id}/finish`                                | `api.finish`             | `ConversationController.finish`         |
| `POST /api/conversations/{id}/feedback/retry`                        | `api.retryFeedback`      | `ConversationController.retryFeedback`  |
| `POST /api/conversations/{id}/messages/{messageId}/translation`      | `api.translate`          | `ConversationController.translate`      |
| `POST /api/conversations/{id}/messages/{messageId}/word-translation` | `api.translateWord`      | `ConversationController.translateWord`  |
| `POST /api/conversations/{id}/voice-turns`                           | `api.sendVoice`          | `VoiceConversationController.voiceTurn` |
| `POST /api/conversations/{id}/messages/{messageId}/speech`           | `api.speech`             | `VoiceConversationController.speech`    |

## 6. 変更・調査の手順

1. 上の「画面別」表から対象コンポーネントを特定する。
2. JSXの `onClick`、`onSubmit`、`onChange`、`onKeyDown`、`useEffect` からイベントハンドラーを探す。
3. ハンドラーが更新する `useState` と、JSX側の表示条件を確認する。
4. `api.*` を呼んでいる場合は `frontend/src/shared/api.ts` でPath、Method、Request、Response型を確認する。
5. 対応するControllerからServiceへ進み、許可条件、状態遷移、例外を確認する。
6. 保存を伴う場合はEntity、Repository、Migrationを確認する。
7. DTOとFrontend型に同じ項目・null許可・状態値が定義されているか確認する。
8. 対応するコンポーネントテスト、Integration Test、必要に応じてE2Eを更新する。

変更後の基本確認:

```bash
./gradlew :backend:test :backend:spotlessCheck
npm test --prefix frontend
npm run typecheck --prefix frontend
npm run lint --prefix frontend
npm run format:check
```

画面遷移、Cookie / CSRF、非同期処理、音声操作を変更した場合は、アプリを起動して `npm run e2e --prefix frontend` も実行する。

## 7. よく使う検索

```bash
# 画面イベントとReact state
rg -n "useState|useEffect|onClick|onSubmit|onChange|onKeyDown" frontend/src

# Frontend API呼び出し
rg -n "api\." frontend/src

# Backend API入口
rg -n "@(Get|Post|Put|Delete)Mapping" backend/src/main/java

# 永続状態の利用箇所
rg -n "ConversationStatus|FeedbackStatus|EnglishLevel" backend/src frontend/src

# Entity更新メソッドの呼び出し元
rg -n "\.end\(|\.complete\(|\.fail\(|\.retry\(|selectEnglishLevel" backend/src
```
