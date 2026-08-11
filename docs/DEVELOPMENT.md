<!-- TalkOnのセットアップ・起動・検証方法を説明します。開発環境を迷わず再現できるようにする手順書です。 -->

# TalkOn Phase 1 開発・起動手順

## 必要な環境

- Java 21
- Node.js 22 / npm 10
- Java 21とNode.js 22だけで実行できます。DBは組み込みH2です。

## 初回セットアップと起動

```bash
npm install
npm run setup
npx --prefix frontend playwright install chromium
npm run dev
```

ブラウザで `http://localhost:5173` を開きます。Spring Bootは `http://localhost:8080` で同時起動します。`OPENAI_API_KEY` を空にすると、ローカル開発・動作確認用の決定的Fake LLMが使われます。実際のOpenAI互換APIを使う場合は環境変数へキーを設定してください。秘密情報をコミットしないでください。

OpenAPI UIは `http://localhost:8080/swagger-ui/index.html`、仕様JSONは `http://localhost:8080/v3/api-docs` です。

## 環境変数

| 変数              | 説明                              | 既定値                          |
| ----------------- | --------------------------------- | ------------------------------- |
| `DB_URL`          | JDBC URL                          | リポジトリ内 `data/talkon` のH2 |
| `DB_USER`         | DBユーザー                        | `sa`                            |
| `DB_PASSWORD`     | DBパスワード                      | 空                              |
| `CORS_ORIGIN`     | 許可するフロントOrigin（単一）    | `http://localhost:5173`         |
| `COOKIE_SECURE`   | HTTPS環境でCookieへSecureを付与   | `false`                         |
| `OPENAI_API_KEY`  | OpenAI互換APIキー。空ならFake LLM | 空                              |
| `OPENAI_BASE_URL` | OpenAI互換APIのベースURL          | `https://api.openai.com/v1`     |
| `OPENAI_MODEL`    | 使用モデル                        | `gpt-4.1-mini`                  |
| `VITE_API_URL`    | ブラウザから見たAPI URL           | `http://localhost:8080`         |

## テスト

```bash
npm test
npm run lint
npm run typecheck
npm run build
cd frontend
npm test
npm run e2e
```

PlaywrightのBrowserが未導入の場合、E2Eはアプリへ接続する前に失敗します。初回セットアップ時またはPlaywright更新後は、リポジトリルートで `npx --prefix frontend playwright install chromium` を実行してください。E2E実行時は別Terminalで `npm run dev` を起動しておきます。

通常のテストはH2とFake LLMを利用し、外部LLM APIを呼びません。E2Eは `npm run dev` でアプリ全体を起動してから実行します。
