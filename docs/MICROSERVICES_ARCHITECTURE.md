<!-- TalkOnをモノリスからマイクロサービスへ安全に移行するための、境界・通信・データ所有・段階移行を定義する設計書です。 -->

# TalkOn マイクロサービスアーキテクチャ設計書

## 1. 文書の位置づけ

この文書は、TalkOnを一括分割せず、Strangler Figパターンで段階的にマイクロサービスへ移行するための基準を定める。現在の実装は第1段階として音声基盤を独立サービスへ切り出している。

マイクロサービス化の目的は、コード量を分割することではない。次の運用上の境界を独立させることにある。

- 障害の影響範囲
- スケール単位
- デプロイ単位
- 外部APIと秘密情報
- データの所有権
- 変更頻度と担当責務

イベント1件ごとにはサービスを作らない。同じ整合性境界とデータを共有する処理は同じサービスに置く。

## 2. 現在の構成

```text
Browser :5173
  │ public HTTP / Cookie / CSRF
  ▼
Core Backend :8080
  ├─ Identity / Profile
  ├─ Conversation / Message
  ├─ Feedback / Translation / Dashboard（移行前）
  ├─ Core DB（H2）
  └─ internal HTTP + X-Internal-Service-Key
       ▼
Speech Service :8081
  ├─ Speech-to-Text
  ├─ Text-to-Speech
  └─ OpenAI Audio API
```

現時点は「独立した音声マイクロサービス + 残存コアモノリス」の移行アーキテクチャである。FrontendはCore Backendだけを公開APIとして利用し、Speech Serviceを直接呼ばない。

## 3. サービス境界

### 3.1 目標サービス

| サービス             | 責務                                                 | 所有データ                           | 主な入力            | 主な出力                          |
| -------------------- | ---------------------------------------------------- | ------------------------------------ | ------------------- | --------------------------------- |
| Edge / BFF           | Browser向けAPI、認証コンテキスト伝播、レスポンス集約 | なし                                 | Public HTTP         | 画面用レスポンス                  |
| Identity Service     | 登録、ログイン、プロフィール、英語レベル             | User、Credential、Session            | 認証コマンド        | User identity                     |
| Conversation Service | 会話ライフサイクル、メッセージ順序、所有者判定、履歴 | Conversation、Message                | 会話コマンド        | Conversation detail、domain event |
| AI Tutor Service     | 挨拶、返信、翻訳、モデル利用量                       | Prompt version、必要ならusage ledger | AI生成コマンド      | 生成テキスト、usage               |
| Speech Service       | STT、TTS、音声プロバイダー接続                       | 永続データなし                       | 音声・英文          | Transcript・音声                  |
| Feedback Service     | 終了会話の非同期評価、再生成                         | Feedback                             | ConversationEnded   | Feedback status / result          |
| Learning Service     | 学習時間、継続日数、アクティビティ集計               | Read model                           | Conversation events | Dashboard projection              |

### 3.2 分割しない境界

- ConversationとMessageは、順序・上限・ACTIVE判定を同一トランザクションで守るため同じサービスに置く。
- STTとTTSは同じ外部プロバイダー設定、料金・再試行・スケール特性を持つためSpeech Serviceにまとめる。
- Feedbackの各カテゴリを別サービスにはしない。1回の評価結果として整合性を持つ。
- Dashboardの各カードを別サービスにはしない。同じ学習Read Modelから取得する。

## 4. イベントと責務の割り当て

| ユーザー／ドメインイベント | 実行責務                   | 同期通信                           | 発行イベント                           | 失敗時の方針                       |
| -------------------------- | -------------------------- | ---------------------------------- | -------------------------------------- | ---------------------------------- |
| UserRegistered             | Identity                   | なし                               | `UserRegistered`                       | 登録全体を失敗                     |
| EnglishLevelSelected       | Identity                   | なし                               | `UserProfileUpdated`                   | 更新全体を失敗                     |
| ConversationStarted        | Conversation               | AI Tutorへ挨拶生成                 | `ConversationStarted`                  | AI失敗時は開始失敗                 |
| MessageSent                | Conversation               | AI Tutorへ返信生成                 | `MessageAdded`                         | USER発言を残し再試行可能にする     |
| VoiceTurnRequested         | Conversation orchestration | Speech STT → AI Tutor → Speech TTS | `MessageAdded`                         | STT失敗は中止、TTS失敗は英文を返す |
| ConversationFinished       | Conversation               | なし                               | `ConversationEnded`                    | 会話終了を確定して後続処理から分離 |
| FeedbackRequested          | Feedback                   | AI Tutorへ評価生成                 | `FeedbackCompleted` / `FeedbackFailed` | FAILEDとして保存し再実行可能にする |
| TranslationRequested       | AI Tutor                   | Conversationから対象英文取得       | 必要なら `TranslationCompleted`        | 画面へ再試行可能なエラー           |
| DashboardRequested         | Learning                   | なし                               | なし                                   | 最終反映時点のRead Modelを返す     |

## 5. 通信設計

### 5.1 同期通信を使う処理

ユーザーが応答を待っており、その場で結果が必要な処理だけ内部HTTPを使う。

- STT
- TTS
- AI会話返信
- 単語・文章翻訳

内部APIはPublic APIとURL空間を分け、`/internal/**` とする。現在のSpeech Serviceは `X-Internal-Service-Key` で呼び出し元を確認する。本番では平文の固定キーだけに依存せず、private network + mTLSまたはworkload identityへ移行する。

### 5.2 非同期イベントを使う処理

ユーザー操作のトランザクションから分離できる処理はイベントで連携する。

- 会話終了後のFeedback生成
- Dashboard Read Model更新
- 利用量・監査ログ集計

目標構成ではKafka、RabbitMQ、またはmanaged queueを利用する。導入前はTransactional OutboxテーブルをConversation Serviceへ置き、DB更新とイベント記録を同一トランザクションにする。ConsumerはイベントIDで冪等処理する。

## 6. データ所有権

### 原則

- サービスごとにDBを所有し、他サービスのテーブルを直接参照しない。
- 外部キーはサービス境界を越えて張らない。外部IDとして保持する。
- 一覧・集計は同期的な分散JOINを避け、イベントからRead Modelを作る。
- APIレスポンス用の複合データはBFFが集約する。

### 移行マッピング

| 現在のテーブル           | 将来の所有サービス | 移行方法                                 |
| ------------------------ | ------------------ | ---------------------------------------- |
| `users`                  | Identity           | 最後に認証Sessionと合わせて分離          |
| `conversation_sessions`  | Conversation       | 会話API切替時に専用DBへ移行              |
| `conversation_messages`  | Conversation       | Sessionと同時移行                        |
| `conversation_feedbacks` | Feedback           | `ConversationEnded` Consumer導入後に移行 |
| Dashboard集計            | Learning           | Event projectionとして新規作成           |

Speech Serviceは音声ファイルを永続化しない。受信音声と生成音声はリクエスト処理中だけ保持する。

## 7. Speech Serviceの実装契約

### 内部API

| Method | Path                              | Request              | Response                                     |
| ------ | --------------------------------- | -------------------- | -------------------------------------------- |
| POST   | `/internal/speech/transcriptions` | multipart `audio`    | `{ "text": string, "model": string }`        |
| POST   | `/internal/speech/synthesis`      | `{ "text": string }` | 音声binary、`Content-Type`、`X-Speech-Model` |

共通Header:

```text
X-Internal-Service-Key: ${INTERNAL_SERVICE_KEY}
```

### コード配置

| 責務                          | ファイル                                                                              |
| ----------------------------- | ------------------------------------------------------------------------------------- |
| 独立プロセス入口              | `speech-service/src/main/java/com/talkon/speechservice/SpeechServiceApplication.java` |
| 内部API                       | `speech-service/src/main/java/com/talkon/speechservice/SpeechController.java`         |
| 内部認証                      | `speech-service/src/main/java/com/talkon/speechservice/InternalKeyInterceptor.java`   |
| OpenAI / Local実装            | `speech-service/src/main/java/com/talkon/speechservice/SpeechProviderConfig.java`     |
| Backend側Client               | `backend/src/main/java/com/talkon/speech/RemoteSpeechClientConfig.java`               |
| Backend側オーケストレーション | `backend/src/main/java/com/talkon/speech/VoiceConversationService.java`               |

## 8. 障害・可用性設計

| 障害                   | ユーザーへの結果                | 将来追加する対策                     |
| ---------------------- | ------------------------------- | ------------------------------------ |
| Speech Service接続不可 | STTは503、TTSは英文を残して警告 | timeout、circuit breaker、メトリクス |
| OpenAI STT失敗         | 音声ターンを中止                | 制限付きretry、DLQは使用しない       |
| OpenAI TTS失敗         | AI英文を成功扱い、音声だけ警告  | 再読み上げ操作                       |
| Feedback Consumer失敗  | FeedbackをFAILED                | retry + DLQ + 管理画面               |
| Event重複              | 同じ結果を再生成しない          | event IDによる冪等性                 |
| Learning Service遅延   | 少し古いDashboardを表示         | projection lag表示・監視             |

内部HTTPには接続・読込timeoutを必ず設定する。無制限retryは連鎖障害を起こすため禁止する。

## 9. セキュリティ

- Browserから到達できるのはEdge / BFFだけにする。
- 内部サービスをprivate networkへ置く。
- `OPENAI_API_KEY` はAI Tutor / Speechなど利用サービスだけへ渡す。
- 内部通信のユーザーIDは信用せず、BFFが署名した短命tokenまたはservice identityを使用する。
- ログへ音声binary、会話全文、password、API keyを出さない。
- 本番の `INTERNAL_SERVICE_KEY` に既定値を使用しない。

## 10. 可観測性

すべての公開・内部リクエストで次を引き回す。

- `X-Request-ID`
- trace ID
- 呼び出し元service名
- event ID / conversation ID（本文は出さない）

サービス別にlatency、成功率、timeout数、OpenAI呼び出し回数、retry数を記録する。分散トレーシングはOpenTelemetryを利用する。

## 11. 移行ロードマップ

| Phase | 内容                        | 完了条件                                                      | 状態     |
| ----- | --------------------------- | ------------------------------------------------------------- | -------- |
| 1     | Speech Service分離          | 別port・別Boot module、内部HTTP、独立テスト                   | 実装済み |
| 2     | モノリス内部のPort整理      | ConversationからAI・Feedback・Dashboardの直接依存をPortへ限定 | 未着手   |
| 3     | Feedback Service + Outbox   | 終了イベント、非同期Consumer、専用DB                          | 未着手   |
| 4     | Learning Service            | 会話イベントからDashboard projectionを構築                    | 未着手   |
| 5     | AI Tutor Service            | 会話生成・翻訳・Feedback生成のモデル通信を集約                | 未着手   |
| 6     | Identity / Conversation分離 | DB分離、BFF導入、Session方式変更                              | 未着手   |
| 7     | 運用基盤                    | service discovery、secret管理、trace、deploy、rollback        | 未着手   |

各Phaseは既存API契約を維持し、Frontendを同時変更しなくてもよい形で進める。

## 12. ローカル実行

`npm run dev` は次の3プロセスを起動する。

| プロセス       | Port | 公開範囲       |
| -------------- | ---- | -------------- |
| Frontend       | 5173 | Browser        |
| Core Backend   | 8080 | Browser向けAPI |
| Speech Service | 8081 | 内部APIのみ    |

BackendとSpeech Serviceには同じ `INTERNAL_SERVICE_KEY` を設定する。Backendの接続先は `SPEECH_SERVICE_URL` で変更できる。

## 13. テスト戦略

- Speech Service単体: 内部認証、STT/TTS API、Local Provider
- Core Backend単体: Speech Portをmockし、部分失敗を確認
- Contract Test: Backend ClientとSpeech Serviceのrequest / response互換性
- Integration Test: 実際に2サービスを起動した音声ターン
- E2E: Frontendから録音、Transcript、AI英文、再生まで

PR単位では少なくとも両Spring Boot moduleのtestとformat checkを実行する。
