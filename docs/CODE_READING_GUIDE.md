<!-- TalkOn Phase 1.5のコードを読む順番と処理のつながりを説明します。初めてリポジトリを読む人向けの参考書です。 -->

# TalkOn コード読解ガイド

## 1. 最初に知る構成

TalkOnはJava/Spring BootのバックエンドとTypeScript/Reactのフロントエンドを一つのリポジトリで管理するモノリスです。

```text
frontend/src
  ↓ HTTP / JSON
backend/src/main/java
  ↓ JPA
H2 Database
  ↓ 必要な処理だけ
OpenAI互換API または Fake LLM
```

## 2. おすすめの読む順番

1. `frontend/src/App.tsx` で画面とURLの関係を見る
2. `frontend/src/shared/api.ts` でブラウザとBackendの契約を見る
3. 対象のControllerでAPI入口を見る
4. Serviceで業務処理と状態遷移を見る
5. EntityとRepositoryで保存内容を見る
6. `llm/ConversationAiClient.java` でAI境界を見る
7. `llm/AiClientConfig.java` と `llm/Prompts.java` でAI実装を見る
8. Reactの `features` 配下で表示を追う
9. Integration Testで期待する一連の動作を確認する

## 3. 初回オンボーディングを追う

```text
App.tsx
  user.englishLevel が null
  ↓
EnglishLevelPage.tsx
  ↓ api.selectEnglishLevel
PUT /api/users/me/english-level
  ↓
UserProfileController.java
  ↓
User.selectEnglishLevel
```

データ型はBackendの `EnglishLevel.java` とFrontendの `EnglishLevel` 型が対応します。DB列はFlyway V3で追加されます。

## 4. 会話開始と返信を追う

会話開始は `DashboardPage.tsx` のボタンから始まります。

```text
api.start()
  ↓ POST /api/conversations
ConversationController.start
  ↓
ConversationService.start
  ├─ UserのEnglishLevelを取得
  ├─ ai.greeting(level)
  └─ Sessionと最初のASSISTANT Messageを保存
```

返信ではUser Messageを先に保存し、保存済み履歴とEnglishLevelを `ai.reply(messages, level)` へ渡します。AI失敗時にはUser Messageが履歴へ残る点も確認してください。

Conversation AIの人格は `Prompts.CONVERSATION`、難易度差は `Prompts.levelPolicy` にあります。OpenAIとFakeの切替は `AiClientConfig.client()` です。

## 5. 翻訳を追う

`MessageList.tsx` が表示の開閉をローカルStateで管理します。未翻訳の場合だけAPIを呼びます。

```text
MessageList.showTranslation
  ↓ api.translate
ConversationController.translate
  ↓
ConversationService.translate
  ├─ 会話所有者を確認
  ├─ ASSISTANT Messageか確認
  ├─ 保存済みなら再利用
  └─ 未保存ならAI翻訳してMessageへ保存
```

翻訳の開閉はFrontendだけの状態であり、翻訳本文はDBへ保存されます。

## 6. 会話終了とFeedbackを追う

ここは同期処理と非同期処理の境界が重要です。

```text
ConversationService.finish
  ├─ ConversationをENDEDへ変更
  ├─ FeedbackをGENERATINGで保存
  └─ DB commit後に非同期生成を開始
        ↓
FeedbackGenerationService.generate
  ├─ AI生成とvalidationを最大2回試す
  ├─ 成功ならCOMPLETED
  └─ 失敗ならFAILED
```

Frontendの `App.tsx` はGENERATING中に詳細APIをpollingします。`FeedbackPanel.tsx` がGENERATING、FAILED、COMPLETEDを分けて表示します。

Feedbackの教材データ型は `FeedbackData.java` です。`original == corrected` はrecord生成時に除外されます。EntityはJSON文字列として保存し、`ConversationDtos.feedback()` がAPI用JSON配列へ戻します。

## 7. Dashboardを追う

`DashboardService.java` は直近52週のConversationを日付別にJavaで集計します。各Sessionの時間とMessage数を合算し、`DashboardResponse.DailyActivity` を返します。

Frontendでは `ActivityGrid.tsx` が7行のCSS Gridへ変換します。各マスの `data-tooltip` が日付、Conversation数、Message数を持ち、CSSの `:hover` と `:focus` で表示されます。

## 8. 認証と所有者チェック

認証情報はHTTP Sessionに保存されます。Controllerでは `CurrentUser.require(authentication)` からユーザーIDを取得します。会話詳細、翻訳、終了では必ず `ConversationService.owned()` または `locked()` を通り、他ユーザーのConversationを404として隠します。

## 9. DB Migrationの読み方

- V1: 初期User、Conversation、Message、Feedback
- V2: Feedback状態分離とTranslation
- V3: EnglishLevelとvocabularyTips

適用済みMigrationはコメント変更だけでもチェックサムが変わるため編集しません。スキーマ変更は常に新しい番号のファイルへ追加します。

## 10. テストを参考書として使う

- `AuthIntegrationTest.java`: 登録、ログイン、Level保存
- `ConversationIntegrationTest.java`: 会話、所有者、終了、Feedback、Translation、Dashboard
- `FeedbackDataTest.java`: 無意味なCorrection除外
- `EnglishLevelPage.test.tsx`: オンボーディング
- `ConversationSupport.test.tsx`: Translationと教材Feedback
- `DashboardPage.test.tsx`: 学習情報とTooltip
- `happy-path.spec.ts`: 登録からFeedbackまでのブラウザ正常系

処理を変更する際は、まず対応するテストで現在の契約を確認し、変更後の期待をテストへ追加してから実装を追うと理解しやすくなります。
