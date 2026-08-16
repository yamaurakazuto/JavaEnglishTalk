<!-- TalkOnの概要、起動方法、構成、運用上の注意を案内します。初めて触る人が迷わず動かせる入口として配置しています。 -->

# TalkOn

TalkOnは、AIとの英会話、音声入力、読み上げ、翻訳、フィードバック、学習記録を一つにまとめた英語学習アプリです。

現在はMVPフェーズ2まで実装済みです。JavaとTypeScript／Reactで構成されたモノリスとして、ローカル環境で動作します。

## 主な機能

- AIとの自由英会話
- マイクを使った英語音声入力
- AI英文の音声読み上げ
- AI英文の日本語翻訳
- 会話終了後の学習フィードバック
- 会話履歴の保存と振り返り
- 今日の学習時間、継続日数、学習日の表示
- GitHubの草に似た学習アクティビティ表示
- 会話ごとの使用トークン数と概算料金表示
- ローカル開発時の自動ログイン

## 使用技術

| 分類           | 技術                                                   |
| -------------- | ------------------------------------------------------ |
| バックエンド   | Java 21、Spring Boot、Spring Security、Spring Data JPA |
| フロントエンド | TypeScript、React、Vite                                |
| データベース   | H2 Database、Flyway                                    |
| AI会話         | OpenAI Chat Completions API、GPT-5.6 Luna              |
| 音声認識       | OpenAI Audio API、GPT-4o mini Transcribe               |
| 音声合成       | OpenAI Audio API、GPT-4o mini TTS                      |
| テスト         | JUnit、Spring MockMvc、Vitest、Playwright              |
| コード整形     | Spotless、Google Java Format、Prettier                 |

## ディレクトリ構成

```text
EnglishTalkapp/
├── backend/            Spring Bootバックエンド
│   ├── src/main/       Javaコード、設定、DBマイグレーション
│   ├── src/test/       バックエンドテスト
│   └── data/           ローカルH2データベース
├── frontend/           Reactフロントエンド
│   ├── src/            画面、機能、APIクライアント
│   └── e2e/            E2Eテスト
├── docs/               設計書、実装書、コードリーディング資料
├── .env.example        ローカル環境変数の見本
└── package.json        モノリス全体の実行コマンド
```

## 必要なもの

- Java 21以上
- Node.js 20以上
- npm
- OpenAI APIキー
- OpenAI APIの利用可能なクレジット

Dockerは現在のローカル実行には使用しません。

## 初回セットアップ

### 1. 依存関係をインストールする

リポジトリのルートで実行します。

```bash
npm install
npm run setup
```

### 2. 環境変数ファイルを作成する

```bash
cp .env.example .env
```

`.env`へ、OpenAI Platformで発行したAPIキーを設定します。

```env
OPENAI_API_KEY=your_openai_api_key
```

APIキーは`.env.example`、GitHub、チャット、スクリーンショットへ掲載しないでください。`.env`はGitの管理対象外です。

### 3. OpenAIモデルを設定する

現在の設定例です。

```env
OPENAI_MODEL=gpt-5.6-luna

OPENAI_STT_MODEL=gpt-4o-mini-transcribe
OPENAI_TTS_MODEL=gpt-4o-mini-tts
OPENAI_TTS_VOICE=coral
OPENAI_TTS_FORMAT=mp3
OPENAI_TTS_SPEED=1.0
```

### 4. 概算料金用の単価を設定する

TalkOnの画面表示に使用する単価です。OpenAI側の請求単価を変更する設定ではありません。

```env
LLM_INPUT_USD_PER_MILLION=1.00
LLM_OUTPUT_USD_PER_MILLION=6.00
LLM_YEN_PER_USD=160
```

OpenAIの料金は変更される可能性があります。設定前に公式料金を確認してください。

## ローカル自動ログイン

ローカル環境でメールアドレスとパスワードの入力を省略する場合は、`.env`へ次の設定を追加します。

```env
LOCAL_AUTO_LOGIN=true
```

有効にすると、`local@talkon.dev`の開発ユーザーとして自動認証されます。

自動ログインはループバック接続だけで動作します。本番環境では必ず`false`にしてください。

```env
LOCAL_AUTO_LOGIN=false
```

## 起動方法

リポジトリのルートで実行します。

```bash
npm run dev
```

バックエンドとフロントエンドが同時に起動します。

| 対象            | URL                                           |
| --------------- | --------------------------------------------- |
| TalkOn          | <http://localhost:5173>                       |
| バックエンドAPI | <http://localhost:8080>                       |
| Swagger UI      | <http://localhost:8080/swagger-ui/index.html> |

停止するときは、起動したターミナルで`Ctrl + C`を押します。

環境変数を変更した場合は、TalkOnを再起動してください。

## 会話データの保存先

ローカル環境では、ユーザー、会話、メッセージ、翻訳、フィードバック、トークン数をH2 Databaseへ保存します。

```text
backend/data/talkon.mv.db
```

音声ファイル自体は保存しません。マイク音声は文字起こしに使用し、文字列として会話履歴へ保存します。

データベースファイルを削除するとローカルの会話履歴も消えるため、取り扱いに注意してください。このファイルはGitHubには登録されません。

## OpenAI API料金

TalkOnでは、次の処理でOpenAI API料金が発生します。

- Lunaによる挨拶と会話返答
- 日本語翻訳
- 会話終了後のフィードバック
- ユーザー音声の文字起こし
- AI英文の音声合成

画面に表示される概算料金は、現在、会話開始と会話返答のLuna利用分だけです。翻訳、フィードバック、音声認識、音声合成は含まれません。

毎日15分利用する場合の目安は月額約750円から950円です。料金の前提、計算式、利用時間別の目安は[OpenAI API料金ガイド](docs/OPENAI_API_COST_GUIDE.md)を参照してください。

実際の請求額は[OpenAI Platform Usage](https://platform.openai.com/usage)で確認します。

## 開発コマンド

| コマンド               | 内容                                       |
| ---------------------- | ------------------------------------------ |
| `npm run dev`          | バックエンドとフロントエンドを同時起動     |
| `npm test`             | バックエンドとフロントエンドのテストを実行 |
| `npm run typecheck`    | TypeScriptの型チェックを実行               |
| `npm run lint`         | フロントエンドのLintを実行                 |
| `npm run format`       | Java、TypeScript、CSS、Markdownなどを整形  |
| `npm run format:check` | コード整形状態を確認                       |
| `npm run build`        | バックエンドとフロントエンドをビルド       |

## テスト

すべての自動テストを実行します。

```bash
npm test
```

個別に確認する場合は次のコマンドを使用します。

```bash
./gradlew :backend:test
npm test --prefix frontend
npm run typecheck
npm run lint
```

## ドキュメント

| ファイル                                                 | 内容                             |
| -------------------------------------------------------- | -------------------------------- |
| [コードリーディングガイド](docs/CODE_READING_GUIDE.md)   | イベントから処理を追うための案内 |
| [システム設計書](docs/SYSTEM_DESIGN.md)                  | 全体構成と設計方針               |
| [開発ガイド](docs/DEVELOPMENT.md)                        | 開発環境と作業方法               |
| [MVPフェーズ2実装書](docs/MVPフェーズ2実装書.md)         | フェーズ2で実装した内容          |
| [MVPフェーズ2移行書](docs/MVPフェーズ2移行書.md)         | フェーズ2への移行内容            |
| [ダッシュボード作業記録](docs/MVP_DASHBOARD_WORK_LOG.md) | ダッシュボードの設計・実装記録   |
| [OpenAI API料金ガイド](docs/OPENAI_API_COST_GUIDE.md)    | 料金構造と月額目安               |

## 現在の位置づけ

TalkOnは学習用のMVPです。ローカル環境で一連の英会話学習を試せる状態ですが、本番公開に向けては次の対応が必要です。

- H2 Databaseから本番向けデータベースへの移行
- 本番用の認証・Cookie・CORS設定
- OpenAI APIの月次利用上限と監視
- 音声、翻訳、フィードバックを含む料金集計
- Deprecatedモデルの後継モデルへの移行確認
- 本番環境でのログ管理、監視、バックアップ
