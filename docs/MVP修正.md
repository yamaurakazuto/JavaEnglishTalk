<!-- TalkOn MVP改善設計書をもとに行った調査・判断・実装箇所を記録します。問題から実装へ至った理由を後から追跡できるようにする作業書です。 -->

# TalkOn MVP修正 作業書

## 1. この文書の目的

本書は「TalkOn MVP 改善設計書」に基づいて実施した修正について、次の内容を明確にするものです。

- 調査で実際に分かったこと
- 何を考えて設計を決めたか
- どのファイルへ何を実装したか
- 実装後の処理と状態
- テストした内容
- MVPとして残した制約

今回もSpring Boot、Java、TypeScript、Reactのモノリス構成を維持しています。Kafka、Redis、WebFluxなどの新しい基盤は追加していません。

## 2. 調査結果と実装方針

### 2.1 会話終了後の白画面

#### 調査で分かったこと

修正前の `ConversationService.finish()` は、次の処理を一つの同期処理として実行していました。

```text
会話をFEEDBACK_GENERATINGへ変更
  ↓
AIフィードバック生成を待機
  ↓
成功時だけフィードバック保存と会話完了
  ↓
失敗時は会話をACTIVEへ戻して503エラー
```

フロントエンドはAPIエラーを文字で表示していましたが、フィードバックの生成中・失敗状態を画面として表現できませんでした。また、想定外のReact例外を受け止める境界もありませんでした。

ブラウザで再現確認した結果、白画面の直接原因も特定できました。H2のJSON列に保存した配列が、環境によってJSON文字列として二重エンコードされた状態でAPIへ返り、Reactが `feedback.strengths.map(...)` を実行した時に `map is not a function` 例外が発生していました。

#### 判断

ユーザーが行った「会話終了」は、外部AIの成功・失敗に関係なく確定させるべきです。そのため、会話状態とフィードバック生成状態を別々に管理することにしました。

会話終了後は先にレスポンスを返し、AIフィードバックはSpring Boot内の非同期処理で生成します。失敗しても会話はENDEDのまま残し、画面から再生成できるようにしました。

加えて、DTO変換時にフィードバックのJSONを確認し、文字列として二重に包まれている場合はもう一度JSONとして解析します。フロントエンドへは必ず配列を返す契約にそろえました。

### 2.2 AIが同じ発言を繰り返す

#### 調査で分かったこと

OpenAI互換APIの実装には、すでにSystem Promptと会話履歴が渡されていました。そのため、「履歴がまったく渡されていない」という仮説は該当しませんでした。

一方で、次の改善余地がありました。

- System Promptに質問の重複回避が明記されていない
- ユーザーの直前の内容へ具体的に触れる指示が弱い
- ローカルFake AIが毎回同じ固定文を返す
- AIへ渡した履歴件数をログで確認できない

#### 判断

新しい会話管理機能は増やさず、MVP改善設計書の方針どおり「履歴入力 + Prompt改善」を採用しました。

OpenAI互換APIには直近20メッセージを順番どおり渡します。System Promptには、過去の質問を確認すること、ユーザーの最新回答に含まれる具体的な内容へ触れること、関連する新しい質問へ進むこと、短く分かりやすい英文を使うことを追加しました。

ローカルFake AIも最新発言を回答へ含め、会話ターンに応じて質問を切り替えるようにしました。

### 2.3 AI英文を理解できない

#### 調査で分かったこと

修正前はAIメッセージの英文だけが表示され、理解できなかった場合の補助操作がありませんでした。

#### 判断

日本語訳を常時表示すると英語を読む機会を減らすため、AIメッセージごとに「日本語訳を見る」ボタンを配置しました。ホバーではなくボタンにした理由は、PCだけでなくタッチ操作のモバイルでも同じ方法で利用できるためです。

翻訳は最初に必要になった時だけAIへ依頼し、結果をDBへ保存します。同じメッセージを再表示した場合は、保存済み翻訳を使うため再度AIを呼びません。

### 2.4 エラー表示とログ

#### 判断

通常のAPIエラーと予期しないReact例外を分けて扱いました。

- 会話画面は `LOADING`、`SUCCESS`、`ERROR` を明示する
- フィードバックは `GENERATING`、`COMPLETED`、`FAILED` を表示する
- 想定外のReact例外はError Boundaryで案内画面へ置き換える
- 全APIリクエストへrequestIdを付け、HTTP結果と処理時間を記録する
- 会話終了、送信、AI処理、フィードバック生成の結果と処理時間を記録する

ログへ会話本文や翻訳対象本文は出していません。問題追跡に必要なID、状態、件数、処理時間を残しつつ、ユーザーの会話内容を不要に記録しないためです。

## 3. 修正後の処理

### 3.1 会話終了とフィードバック生成

```text
POST /api/conversations/{id}/finish
  ↓
Conversation.status = ENDED
Conversation.finishedAt = 現在日時
Feedback.status = GENERATING
  ↓
HTTP 202を返す
  ↓
トランザクション確定後に非同期生成
  ├─ 成功: Feedback.status = COMPLETED
  └─ 失敗: Feedback.status = FAILED
```

画面はフィードバックがGENERATINGの間、1秒ごとに会話詳細を再取得します。COMPLETEDなら内容を表示し、FAILEDなら「もう一度試す」ボタンを表示します。

### 3.2 会話応答

```text
System Prompt
  +
直近20件のUSER・ASSISTANT履歴
  ↓
AI応答生成
  ↓
応答を会話履歴へ保存
```

20件に制限した理由は、数ターン分の文脈を保ちながら、会話が長くなった場合の入力サイズ増加を抑えるためです。

### 3.3 任意翻訳

```text
AIメッセージの「日本語訳を見る」を押す
  ↓
POST /api/conversations/{conversationId}/messages/{messageId}/translation
  ↓
会話所有者とAIメッセージであることを確認
  ↓
保存済み翻訳あり: そのまま返す
保存済み翻訳なし: AI翻訳後にDB保存して返す
```

## 4. 作業ファイル一覧

### 4.1 バックエンド

| 種別 | ファイル                                                                                   | 実装・修正内容                                               |
| ---- | ------------------------------------------------------------------------------------------ | ------------------------------------------------------------ |
| 変更 | `backend/src/main/java/com/kazuto/talkon/TalkOnApplication.java`                           | Springの非同期処理を有効化                                   |
| 変更 | `backend/src/main/java/com/kazuto/talkon/conversation/ConversationController.java`         | 終了APIを202応答へ変更し、再生成APIと翻訳APIを追加           |
| 変更 | `backend/src/main/java/com/kazuto/talkon/conversation/ConversationService.java`            | 会話終了とAI生成の分離、再生成、翻訳、処理ログを実装         |
| 変更 | `backend/src/main/java/com/kazuto/talkon/conversation/ConversationStatus.java`             | 会話状態をACTIVEとENDEDへ整理                                |
| 変更 | `backend/src/main/java/com/kazuto/talkon/conversation/ConversationSession.java`            | 会話をENDEDへ確定する状態遷移を実装                          |
| 変更 | `backend/src/main/java/com/kazuto/talkon/conversation/ConversationMessage.java`            | 日本語訳の保存項目と更新処理を追加                           |
| 変更 | `backend/src/main/java/com/kazuto/talkon/conversation/ConversationMessageRepository.java`  | 会話IDとメッセージIDを使う所有範囲内検索を追加               |
| 変更 | `backend/src/main/java/com/kazuto/talkon/conversation/ConversationDtos.java`               | 翻訳、状態、エラーを追加し、H2の二重JSONを配列へ正規化       |
| 新規 | `backend/src/main/java/com/kazuto/talkon/feedback/FeedbackStatus.java`                     | GENERATING、COMPLETED、FAILEDを定義                          |
| 変更 | `backend/src/main/java/com/kazuto/talkon/feedback/ConversationFeedback.java`               | 生成状態、失敗内容、完了・失敗・再試行の状態遷移を追加       |
| 新規 | `backend/src/main/java/com/kazuto/talkon/feedback/FeedbackGenerationService.java`          | AIフィードバックの非同期生成、1回再試行、結果保存を実装      |
| 変更 | `backend/src/main/java/com/kazuto/talkon/llm/ConversationAiClient.java`                    | 翻訳処理をAI境界へ追加                                       |
| 変更 | `backend/src/main/java/com/kazuto/talkon/llm/AiClientConfig.java`                          | 履歴範囲、Fake応答、翻訳、AI処理時間ログを実装               |
| 変更 | `backend/src/main/java/com/kazuto/talkon/llm/Prompts.java`                                 | 重複回避・簡単な英語・文脈継続と翻訳用Promptを追加           |
| 新規 | `backend/src/main/java/com/kazuto/talkon/common/RequestLoggingFilter.java`                 | requestId、HTTP Status、処理時間の共通ログを追加             |
| 変更 | `backend/src/main/java/com/kazuto/talkon/common/ApiExceptionHandler.java`                  | 想定外例外をStack Trace付きでログ出力                        |
| 新規 | `backend/src/main/resources/db/migration/V2__separate_feedback_status_and_translation.sql` | フィードバック状態、エラー、翻訳列と旧状態移行を追加         |
| 変更 | `backend/src/test/java/com/kazuto/talkon/ConversationIntegrationTest.java`                 | 非同期終了、履歴を使った応答、翻訳、所有者制御のテストを追加 |

### 4.2 フロントエンド

| 種別 | ファイル                                                          | 実装・修正内容                                           |
| ---- | ----------------------------------------------------------------- | -------------------------------------------------------- |
| 変更 | `frontend/src/shared/api.ts`                                      | 新しい状態と翻訳を型定義し、再生成・翻訳APIを追加        |
| 変更 | `frontend/src/App.tsx`                                            | 読込状態、非同期フィードバックのポーリング、再生成を実装 |
| 新規 | `frontend/src/features/conversation/MessageList.tsx`              | AIメッセージと任意翻訳ボタンを表示                       |
| 新規 | `frontend/src/features/conversation/FeedbackPanel.tsx`            | 生成中・完了・失敗と再生成操作を表示                     |
| 新規 | `frontend/src/features/conversation/ConversationSupport.test.tsx` | 翻訳操作とフィードバック状態表示をテスト                 |
| 変更 | `frontend/e2e/happy-path.spec.ts`                                 | 非同期フィードバックと新しい会話応答に正常系テストを対応 |
| 新規 | `frontend/src/ErrorBoundary.tsx`                                  | 想定外のReact例外を白画面ではなく復帰案内へ変換          |
| 変更 | `frontend/src/main.tsx`                                           | アプリ全体をError Boundaryで囲む                         |
| 変更 | `frontend/src/styles.css`                                         | 翻訳、生成状態、致命的エラー画面のスタイルを追加         |

### 4.3 ドキュメント

| 種別 | ファイル          | 実装・修正内容                         |
| ---- | ----------------- | -------------------------------------- |
| 新規 | `docs/MVP修正.md` | 今回の調査、判断、実装箇所、結果を記録 |

## 5. API契約の変更

### 5.1 会話終了

`POST /api/conversations/{id}/finish` は、フィードバック完了を待たずHTTP 202を返します。

```json
{
  "status": "ENDED",
  "feedback": {
    "status": "GENERATING",
    "summary": null,
    "strengths": [],
    "improvements": [],
    "corrections": [],
    "overallComment": null,
    "errorMessage": null
  }
}
```

### 5.2 フィードバック再生成

```text
POST /api/conversations/{id}/feedback/retry
```

FAILED状態の場合だけ再生成を受け付け、HTTP 202を返します。

### 5.3 AIメッセージ翻訳

```text
POST /api/conversations/{conversationId}/messages/{messageId}/translation
```

対象メッセージと保存済み日本語訳を返します。他ユーザーの会話やUSERメッセージは翻訳できません。

## 6. データベース変更

既存の `V1__initial_schema.sql` は変更せず、新しい `V2__separate_feedback_status_and_translation.sql` を追加しました。適用済みマイグレーションのチェックサムを壊さないためです。

追加・変更内容は次の通りです。

- `conversation_feedbacks.status`
- `conversation_feedbacks.error_message`
- 生成中に内容がなくても保存できるようフィードバック本文列をNULL許可
- `conversation_messages.translation`
- 旧 `FEEDBACK_GENERATING` と `COMPLETED` 会話を `ENDED` へ移行

## 7. 採用しなかった案

### 会話終了APIでAI完了まで待つ

外部AIの遅延や障害が会話終了を失敗させるため採用しませんでした。

### Kafkaなどのメッセージ基盤を導入する

現在は一つのSpring Bootアプリで必要な責務分離が可能です。永続キューが必要だと確認できていないため導入しませんでした。

### 翻訳を常時表示する

英語を読む前に日本語だけを見る可能性があるため、明示操作後だけ表示します。

### 重複したAI回答をフロントエンドで隠す

会話文脈が改善されず、保存済み履歴との不整合も起こるため採用しませんでした。

### 全会話履歴を無制限にAIへ渡す

長期会話で入力サイズと待ち時間が増え続けるため、MVPでは直近20件に制限しました。

## 8. 確認内容

実装後は次の内容を確認します。

- 会話終了APIがHTTP 202とENDEDを返す
- フィードバックが非同期でCOMPLETEDへ変わる
- 終了後の会話へメッセージを送れない
- AI応答が最新ユーザー発言を参照する
- AIメッセージを日本語へ翻訳できる
- 他ユーザーの会話詳細を取得できない
- 生成中と失敗を画面に表示できる
- 失敗画面に再生成ボタンがある
- JavaとReactのテスト、静的解析、型検査、ビルドが成功する

## 9. MVPとして残した制約

- 非同期処理は同一Spring Bootプロセス内で実行するため、プロセス停止時の処理再開保証はありません。
- 生成中のフィードバックを起動時に自動再開する処理はありません。
- AI回答の意味的な重複を機械判定する機能はありません。
- 翻訳は文章全体のみで、単語・文法解説はありません。
- ユーザーごとの英語レベル設定はありません。
- ログはアプリケーションログへ出力し、外部のログ収集基盤は追加していません。

これらは実利用で問題として観測された時点で、記録されたログと利用状況をもとに次の改善対象とします。

---

# Phase 1.5 追加修正

## 10. Phase 1.5で解決する問題

Phase 1.5では、基本機能を増やすのではなく、毎日10〜20分利用できる会話品質・学習品質・UX品質を目標にしました。

- AIが教師のように訂正したり、毎回答質問したりする
- ユーザーの英語レベルに難易度を合わせられない
- Fake LLMの翻訳が日本語訳として使えない
- Feedbackに同一文の無意味なCorrectionが含まれる
- Feedbackが教材として読みづらい
- 会話領域が広く視線移動が大きい
- Dashboardの草から具体的な学習量が分からない

PR #4で追加した非同期Feedback、再試行、Translation API、ログ、Error Boundaryは維持しています。

## 11. Phase 1.5の設計判断

### Conversation AI

AIの役割を「英語教師」ではなく「フレンドリーな英語話者の友達」としました。共通Conversation PolicyとEnglishLevel別Policyを分け、レベルごとの別実装は作っていません。

毎回答質問しない、ユーザー文を機械的に繰り返さない、多少の誤りから意味を推測する、文法修正はFeedbackへ任せる、という責務をSystem Promptへ明記しました。直近20メッセージを渡す既存方針は維持しています。

### EnglishLevel

`BEGINNER`、`INTERMEDIATE`、`ADVANCED` をUserへ保存します。登録項目へ混ぜず、初回ログイン後のオンボーディングで選択します。未選択ユーザーは会話開始できず、選択画面へ誘導されます。

### Translation

翻訳はAIメッセージ単位で必要な時だけ取得します。保存済み翻訳を再利用し、画面では「日本語訳を見る」「日本語訳を閉じる」で開閉できます。Fake LLMも会話で生成する定型応答に対応した自然な日本語を返します。

### Feedback

Feedback AIだけが文法・自然さを評価します。Correctionは `original`、`corrected`、`reasonJa`、`alternative`、`category` を持ちます。DTO生成時にも `original == corrected` を除外し、Promptだけに品質保証を依存させません。明確な誤りがない場合はCorrectionを空にできます。

### DashboardとUI

日別活動へConversation数だけでなくMessage数を追加しました。草はマウスhoverとキーボードfocusの両方で、日付・Conversation数・Message数を表示します。会話本文は760pxへ制限し、入力欄をstickyのまま維持しました。色、角丸、影、余白はCSS変数で共通化を始めています。

### 認証後のCSRF更新

Spring Securityはログイン時にセッションを更新するため、ログイン前に取得したCSRFトークンを使い続けると、直後のレベル保存が失敗します。`frontend/src/shared/api.ts` では登録・ログイン・ログアウトの完了時にトークンキャッシュを破棄し、次の更新通信で新しいトークンを取得する構成にしました。

## 12. Phase 1.5 作業ファイル

| 種別       | ファイル                                                                                    | 内容                                                    |
| ---------- | ------------------------------------------------------------------------------------------- | ------------------------------------------------------- |
| 新規       | `backend/src/main/java/com/kazuto/talkon/user/EnglishLevel.java`                            | 3段階の英会話レベル                                     |
| 変更       | `backend/src/main/java/com/kazuto/talkon/user/User.java`                                    | EnglishLevelの保持と更新                                |
| 新規       | `backend/src/main/java/com/kazuto/talkon/user/UserProfileController.java`                   | レベル保存API                                           |
| 変更       | `backend/src/main/java/com/kazuto/talkon/auth/AuthController.java`                          | Userレスポンスへレベル追加                              |
| 変更       | `backend/src/main/java/com/kazuto/talkon/conversation/ConversationService.java`             | レベル取得とAIへの引き渡し                              |
| 変更       | `backend/src/main/java/com/kazuto/talkon/llm/ConversationAiClient.java`                     | レベル付き会話生成契約                                  |
| 変更       | `backend/src/main/java/com/kazuto/talkon/llm/Prompts.java`                                  | 友達型Conversation Policy、Level Policy、Feedback規則   |
| 変更       | `backend/src/main/java/com/kazuto/talkon/llm/AiClientConfig.java`                           | OpenAI/Fakeのレベル別会話、自然なFake翻訳、教材Feedback |
| 新規       | `backend/src/main/java/com/kazuto/talkon/feedback/FeedbackCategory.java`                    | 修正カテゴリ                                            |
| 変更       | `backend/src/main/java/com/kazuto/talkon/feedback/FeedbackData.java`                        | 新Correction構造と同一文除外                            |
| 変更       | `backend/src/main/java/com/kazuto/talkon/feedback/ConversationFeedback.java`                | vocabularyTips保存                                      |
| 変更       | `backend/src/main/java/com/kazuto/talkon/conversation/ConversationDtos.java`                | 新Feedback API形式                                      |
| 変更       | `backend/src/main/java/com/kazuto/talkon/dashboard/DashboardService.java`                   | 日別Message数集計                                       |
| 変更       | `backend/src/main/java/com/kazuto/talkon/dashboard/DashboardResponse.java`                  | messageCount追加                                        |
| 変更       | `backend/src/main/java/com/kazuto/talkon/conversation/ConversationMessageRepository.java`   | Message数取得                                           |
| 新規       | `backend/src/main/resources/db/migration/V3__add_english_level_and_feedback_vocabulary.sql` | UserレベルとvocabularyTips列                            |
| 変更       | `frontend/src/shared/api.ts`                                                                | Level、Feedback、Dashboardの型とAPI、認証後のCSRF更新   |
| 新規       | `frontend/src/features/onboarding/EnglishLevelPage.tsx`                                     | 初回レベル選択UI                                        |
| 変更       | `frontend/src/App.tsx`                                                                      | オンボーディング制御と会話幅コンテナ                    |
| 変更       | `frontend/src/features/conversation/MessageList.tsx`                                        | 翻訳の取得・開閉                                        |
| 変更       | `frontend/src/features/conversation/FeedbackPanel.tsx`                                      | 教材型Correction UI                                     |
| 変更       | `frontend/src/features/dashboard/ActivityGrid.tsx`                                          | hover/focus Tooltip                                     |
| 変更       | `frontend/src/styles.css`                                                                   | デザイン変数、会話幅、教材、Tooltip、Responsive UI      |
| 変更       | `frontend/e2e/happy-path.spec.ts`                                                           | オンボーディングを含む正常系                            |
| 変更・新規 | `backend/src/test`、`frontend/src/features/**/*.test.tsx`                                   | Level、Feedback、翻訳、Tooltipのテスト                  |
| 変更       | `docs/MVP修正.md`                                                                           | Phase 1.5の判断と実装記録                               |
| 新規       | `docs/CODE_READING_GUIDE.md`                                                                | コードを追うための参考書                                |

## 13. DB・API変更

Flyway V3で `users.english_level` と `conversation_feedbacks.vocabulary_tips` を追加しました。既存V1/V2は変更していません。

追加APIは `PUT /api/users/me/english-level` です。`GET /api/auth/me` を含むUserレスポンスには `englishLevel` が加わります。Dashboardの日別活動には `messageCount` が加わり、Feedbackから旧 `improvements` を外して `corrections` と `vocabularyTips` に整理しました。

## 14. 今回採用しなかったもの

Kafka、Redis、WebFlux、Microservices、音声、CEFR、自動レベル判定、ランキング、決済は導入していません。レベル変更専用設定画面、高度な重複検知、全英文に対応するローカル翻訳エンジンも対象外です。

Fake翻訳はローカルで今回の会話UXを検証できる範囲の決定的実装です。任意英文を高品質に翻訳する場合はOpenAI互換APIを利用します。
