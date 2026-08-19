<!-- TalkOn Phase 2のコードを、要件から画面・API・DB・テストまで迷わず追跡するための実践ガイドです。 -->

# TalkOn コード追跡ガイド

## Phase 2 音声会話を追う

音声会話は次の順で読む。

1. `frontend/src/features/conversation/VoiceRecorder.tsx` が録音開始・停止とUI状態を管理する。
2. `frontend/src/shared/api.ts` の `sendVoice` がBlobをmultipartへ変換する。
3. `VoiceConversationController` が認証ユーザーと音声を受け取る。
4. `VoiceConversationService` が入力検証とSTT → LLM → TTSを調整する。
5. `SpeechRecognitionService` と `TextToSpeechService` が外部APIとの境界になる。
6. `SpeechClientConfig.OpenAiSpeechClient` だけがOpenAIのURLとrequest形式を知る。
7. LLMは既存 `ConversationService.send` を通り、音声文字起こしも通常メッセージと同じ順で保存される。
8. TTS失敗時は `VoiceTurnResult.warning` を返し、ReactはAI英文を残す。

音声再生だけを追う場合は、`MessageList.tsx` の `playSpeech` から `api.speech`、`VoiceConversationController.speech`、`VoiceConversationService.synthesizeMessage` の順に読む。会話所有者とASSISTANTロールを確認してからTTSを呼ぶ。

### Tokenと料金を追う

1. `AiClientConfig.OpenAiClient.chatWithUsage()` がOpenAIレスポンスの入力・出力Tokenを読む。
2. `ConversationAIService.AiResponse` が英文と利用量を会話Serviceへ返す。
3. `ConversationService.recordUsage()` が `LlmCostCalculator` で概算円額を計算する。
4. `ConversationSession.addLlmUsage()` が会話単位で累積する。
5. `ConversationDtos.Detail.llmUsage` がFrontendへ返す。
6. `FeedbackPanel.LlmUsage` が入力、出力、合計Token、概算料金を表示する。
7. 会話中は `App.tsx` の `ConversationUsageBadge` が同じ `llmUsage` を小さく表示する。

翻訳と終了後Feedbackはこの会話料金に含めない。単価は `application.yml` の `app.llm.input-usd-per-million`、`output-usd-per-million`、`yen-per-usd` を確認する。

## 1. このガイドの使い方

この文書は、コードを最初から全行読むためのものではない。「ユーザー操作から保存結果まで」「不具合の表示から原因まで」「変更箇所から必要テストまで」を、境界ごとに追うための地図である。

最初に次の3文書を使い分ける。

| 知りたいこと                   | 参照先                                            |
| ------------------------------ | ------------------------------------------------- |
| 現在の構造・責務・状態         | `docs/SYSTEM_DESIGN.md`                           |
| Phase 2へ移行した理由と残課題  | `docs/MVPフェーズ2移行書.md`                      |
| 実装を読む順番・変更時の確認先 | 本書                                              |
| テストケースと実施記録         | `outputs/mvp-phase2-docs/TalkOnテスト仕様書.xlsx` |

## 2. リポジトリの入口

```text
frontend/src/                  React実装
frontend/e2e/                  ブラウザE2E
backend/src/main/java/         Spring Boot実装
backend/src/main/resources/    設定とFlyway Migration
backend/src/test/              Backendテスト
docs/                          設計・開発・移行資料
```

起動・検査コマンドはルートの `package.json`、個別設定は `backend/build.gradle`、`frontend/package.json`、`application.yml`、`vite.config.ts` を見る。

## 3. 最短で全体をつかむ順番

1. `frontend/src/App.tsx`：URL、認証分岐、画面状態
2. `frontend/src/shared/api.ts`：フロントとBackendの通信契約
3. 対象の `*Controller.java`：HTTP Method、Path、Status
4. 対象の `*Service.java`：業務ルール、transaction、状態遷移
5. `*Repository.java` とEntity：検索条件、所有者条件、保存項目
6. `ConversationDtos.java` または各Response：API JSONへの変換
7. `AiClientConfig.java` と `Prompts.java`：AI入力、履歴、モデル切替
8. 対応するTest：現在保証されている契約

Controllerだけを読んで判断しない。Controllerは薄く、主要な条件分岐はServiceにある。Entityだけを見てもAPIレスポンスは分からないため、DTO変換まで追う。

### クラス・メソッドコメントを入口にする

Javaのクラス、インターフェース、record、enumには、その型が担当する責務と独立している理由を記載している。メソッドコメントは、1文目で「何をするか」、2文目で「なぜ必要か」を説明する。

コメントだけで処理の詳細を判断せず、次の順で読む。

1. 型のコメントで、そのファイルが担当する責務を確認する。
2. 呼び出し元のメソッドコメントで、処理の目的を確認する。
3. メソッド本体で条件分岐、保存、例外処理を確認する。
4. 呼び出されるService、Repository、外部サービス境界へ進む。
5. 対応するテストで、実際に保証されている結果を確認する。

Repositoryメソッドのコメントは検索条件とその条件が必要な理由、Entityの更新メソッドは状態変更をEntityへ集約する理由、外部APIのインターフェースは実装を交換可能にする理由を読む手掛かりにする。

## 4. 1機能を縦に追う基本形

例として「会話を終了」を追う。

```text
表示: App.tsx の「会話を終了」
  ↓ onClick={finish}
Frontend処理: ConversationPage.finish()
  ↓ api.finish(c.id)
HTTP契約: shared/api.ts
  ↓ POST /api/conversations/{id}/finish
API入口: ConversationController.finish()
  ↓ service.finish(id, currentUserId)
業務処理: ConversationService.finish()
  ├─ owned/lockedで所有者確認
  ├─ USER Message件数確認
  ├─ Session.end()
  ├─ ConversationFeedbackをGENERATINGで保存
  └─ commit後にFeedbackGenerationService.generate()
保存: ConversationSession / ConversationFeedback
  ↓
表示更新: FeedbackPanel + App.tsxのpolling
テスト: App.test.tsx / ConversationIntegrationTest.java / happy-path.spec.ts
```

他機能も「表示 → event handler → api.ts → Controller → Service → Repository/Entity → DTO → Test」の順にたどる。

## 5. 機能別の追跡マップ

### 5.1 登録・ログイン

```text
App.AuthForm
  → api.register/login
  → AuthController
  → UserRepository / PasswordEncoder / AuthenticationManager
  → SecurityConfig
  → AuthIntegrationTest
```

Sessionが作成される箇所は `AuthController.login()`、リクエストの認証要否は `SecurityConfig` で確認する。

### 5.2 オンボーディング

```text
App: user.englishLevel判定
  → EnglishLevelPage.select()
  → api.selectEnglishLevel()
  → PUT /api/users/me/english-level
  → UserProfileController
  → User.selectEnglishLevel()
  → V3 Migration
```

ブラウザだけ失敗する場合は、PUT本体だけでなく `SecurityConfig` のCORS allowedMethodsとCSRF Headerを確認する。

### 5.3 会話開始・返信

```text
DashboardPage
  → api.start/send
  → ConversationController
  → ConversationService.start/send
  → ConversationSessionRepository / ConversationMessageRepository
  → ConversationAiClient
  → AiClientConfig.LocalAiClient または OpenAiClient
  → Prompts.CONVERSATION + levelPolicy
```

AIへ何が渡るかを調べる場合は `ConversationService.send()` の履歴取得、`OpenAiClient.reply()` の20件制限、`Prompts` の順で見る。APIキーなしの挙動は `LocalAiClient` が決める。

### 5.4 翻訳

```text
MessageList.showTranslation()
  → api.translate()
  → ConversationController.translate()
  → ConversationService.translate()
  → ConversationMessage.translation
  → Prompts.TRANSLATION
```

翻訳ボタンの開閉はReact State、翻訳結果そのものはDBに保存される。この2種類の状態を混同しない。

### 5.5 フィードバック

```text
ConversationService.finish()
  → ConversationFeedback(GENERATING)
  → FeedbackGenerationService.generate()
  → ConversationAiClient.feedback()
  → Prompts.FEEDBACK
  → FeedbackData Validation
  → ConversationFeedback.complete/fail()
  → ConversationDtos.feedback()
  → FeedbackPanel
```

内容の質は `Prompts.FEEDBACK` とAI実装、生成失敗は `FeedbackGenerationService`、JSON表示エラーは `ConversationDtos.feedback()` とフロント型を確認する。

### 5.6 ダッシュボード

```text
DashboardPage
  → api.dashboard()
  → DashboardController
  → DashboardService
  → ConversationSessionRepository / ConversationMessageRepository
  → StudySummary / ActivityGrid
```

集計期間、日付境界、連続日数は `DashboardService`、見た目とtooltipは `ActivityGrid` を見る。

## 6. 状態から追う

### Conversation

```text
ACTIVE
  └─ finish() → ENDED
```

ACTIVE以外へ送信しようとすると409になる。状態変更は `ConversationSession`、許可条件は `ConversationService` にある。

### Feedback

```text
GENERATING
  ├─ complete() → COMPLETED
  └─ fail()     → FAILED
                     └─ retry() → GENERATING
```

Backend状態は `FeedbackStatus` と `ConversationFeedback`、Frontend表示は `FeedbackPanel` とpolling用 `useEffect` を対応させて読む。

## 7. DBから逆引きする

1. 現在の最終スキーマをV1〜V3の順に合成して考える
2. Tableに対応するEntityを探す
3. Entityを保存・検索するRepositoryを探す
4. Repository Methodを呼ぶServiceを `rg` で探す
5. Serviceを呼ぶControllerとテストへ戻る

例：`conversation_feedbacks.status` を追う場合は、V2 → `ConversationFeedback.status` → `FeedbackGenerationService` → `ConversationDtos.feedback()` → `FeedbackPanel` の順になる。

適用済みMigrationは変更しない。列追加・制約変更は必ず次番号のMigrationを作る。

## 8. 不具合別の調査入口

| 症状                   | 最初に見る場所                                 | 次に確認するもの                           |
| ---------------------- | ---------------------------------------------- | ------------------------------------------ |
| `Failed to fetch`      | Browser Network、`shared/api.ts`               | Backend起動、CORS、OPTIONS、CSRF           |
| 401                    | `SecurityConfig`、`AuthController.login`       | Cookie、credentials、Session               |
| 403                    | CSRF/CORS設定                                  | X-XSRF-TOKEN、Origin、allowedMethods       |
| 404（会話）            | `ConversationService.owned/locked`             | ログインUser IDと会話所有者                |
| 409                    | `ConversationService` の状態検証               | ACTIVE/ENDED、再生成可能状態               |
| 503                    | AI呼び出し周辺ログ                             | API Key、Base URL、timeout、モデル         |
| 会話が不自然           | `LocalAiClient.reply` / `Prompts.CONVERSATION` | 履歴件数、EnglishLevel、モデル             |
| Feedbackが定型         | `LocalAiClient.feedback` / `Prompts.FEEDBACK`  | 入力Transcript、Validation                 |
| Feedbackが生成中のまま | pollingとAsyncログ                             | `@EnableAsync`、transaction commit、status |
| 画面が白い             | Browser Console、`ErrorBoundary`               | API型と実データ、配列のJSON変換            |
| 集計が違う             | `DashboardService`                             | timezone、startedAt/finishedAt、Message数  |

## 9. 変更時に確認する範囲

| 変更対象           | 同時に確認するもの                                     |
| ------------------ | ------------------------------------------------------ |
| API Path / Method  | `shared/api.ts`、CORS、Controller Test、E2E            |
| Request / Response | TypeScript型、DTO、Validation、画面のnull状態          |
| Entity / DB列      | 新規Flyway Migration、Repository、Test schema          |
| Conversation状態   | Service、Controller Status、画面分岐、Integration Test |
| Feedback項目       | `FeedbackData`、Entity JSON、DTO、Frontend型、Panel    |
| AI Prompt          | Local AI、外部AI、品質ケース、token量、JSON parse      |
| 認証               | SecurityConfig、Cookie、CSRF、所有者Test               |
| UI操作             | busy/error state、Component Test、E2E、モバイル幅      |

## 10. テストを仕様として読む

| テスト                         | 主な保証                                              |
| ------------------------------ | ----------------------------------------------------- |
| `AuthIntegrationTest`          | 登録、Hash、重複、認証、CORS、レベル保存              |
| `ConversationIntegrationTest`  | 会話順序、所有者分離、終了、Feedback、Dashboard、翻訳 |
| `FeedbackDataTest`             | 同文訂正の除外                                        |
| `LocalAiClientTest`            | 話題連動応答、複数カテゴリFeedback                    |
| `App.test.tsx`                 | 認証ガード、入力制約、上下の終了導線                  |
| `EnglishLevelPage.test.tsx`    | レベル選択とAPI呼び出し                               |
| `ConversationSupport.test.tsx` | 翻訳、教材Feedback、生成状態                          |
| `DashboardPage.test.tsx`       | 学習集計表示と導線                                    |
| `happy-path.spec.ts`           | 登録からFeedback表示までの通し動作                    |

自動テストがないケースは、Google Sheets向けテスト仕様書の「自動化」列が手動または未実装になっている。変更時は該当行の期待結果を確認し、必要なら自動テストを追加する。

## 11. よく使う検索

```bash
# API入口
rg -n "@GetMapping|@PostMapping|@PutMapping" backend/src/main/java

# 特定APIを呼ぶFrontend
rg -n "api\.finish|api\.send|api\.translate" frontend/src

# Entityや状態の利用箇所
rg -n "FeedbackStatus|ConversationStatus" backend/src

# 全テストケース
rg -n "@Test|test\(" backend/src/test frontend/src frontend/e2e

# 設定値の参照元
rg -n "OPENAI_|CORS_ORIGIN|COOKIE_SECURE|VITE_API_URL" .
```

## 12. 変更後の最小確認

```bash
./gradlew :backend:test :backend:spotlessCheck
npm test --prefix frontend
npm run typecheck --prefix frontend
npm run lint --prefix frontend
npm run format:check
```

ユーザーフロー、Cookie/CORS、非同期処理を変更した場合は、`npm run dev` で全体を起動して `npm run e2e --prefix frontend` も実行する。
