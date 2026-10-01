<!-- TalkOnのMonorepo配置とPackage by Featureの依存ルールを定義します。 -->

# Monorepo・Package by Feature設計書

## 1. 基本方針

TalkOnは、複数の実行アプリを一つのリポジトリで管理するMonorepoとする。リポジトリ直下では実行単位を分離し、各実行単位の内部では技術レイヤーではなく業務機能ごとにコードをまとめる。

```text
EnglishTalkapp/
├── apps/                  ユーザーが直接利用するアプリ
│   └── web/               React Webアプリ
├── services/              独立起動・独立デプロイするBackendサービス
│   ├── core-api/          公開APIとコア業務
│   └── speech-service/    STT・TTS内部サービス
├── docs/                  横断設計・運用資料
├── gradle/                Gradle Wrapper
├── scripts/               Monorepo全体の補助スクリプト
├── settings.gradle        Javaサービスのproject mapping
└── package.json           Monorepo全体の操作コマンド
```

`apps` と `services` の子ディレクトリは、それぞれ単独でビルド可能な実行単位である。サービスを増やす場合は既存サービスの内部へ混在させず、`services/<service-name>` を追加する。

## 2. WebアプリのPackage by Feature

```text
apps/web/src/
├── app/
│   ├── App.tsx                 Routerとアプリ全体の状態
│   ├── App.test.tsx
│   └── ErrorBoundary.tsx
├── features/
│   ├── auth/                   登録・ログイン
│   ├── conversation/           会話・翻訳・音声・Feedback
│   ├── dashboard/              学習状況
│   ├── history/                会話履歴
│   └── onboarding/             英語レベル選択
├── shared/
│   ├── api.ts                  Backend API契約
│   └── ui/                     複数featureで使うUI
├── test/                       テスト共通設定
├── main.tsx                    DOMへのmountだけを担当
└── styles.css
```

依存方向は `app → features → shared` とする。`shared` から `features` を参照しない。あるfeatureだけで使うComponent、hook、型はそのfeature内へ置き、2つ以上のfeatureで安定して共有するときだけ `shared` へ移す。

## 3. Core APIのPackage by Feature

```text
services/core-api/src/main/java/com/talkon/
├── auth/                 認証、Session、CSRF
├── user/                 Userと英語レベル
├── conversation/         会話、Message、翻訳Port
├── feedback/             Feedback生成と永続化
├── dashboard/            学習状況集計
├── speech/               音声会話の公開APIとSpeech Service Client
├── llm/                  AI AdapterとPrompt
├── common/               横断的なエラーとRequest log
└── TalkOnApplication.java
```

Controller、Service、Entity、Repositoryを技術別の共通ディレクトリへ分けず、同じ業務機能のパッケージへ置く。`common` には業務ルールを置かず、複数featureで使う技術的関心事だけを置く。

`llm` は現在AI Adapterを集めた移行中のパッケージである。AI Tutor Serviceを分離するときに独立サービスへ移す。

## 4. Speech ServiceのPackage by Feature

```text
services/speech-service/src/main/java/com/talkon/speechservice/
├── speech/
│   ├── SpeechController.java
│   ├── SpeechProvider.java
│   ├── SpeechProviderConfig.java
│   └── SpeechProperties.java
├── security/
│   ├── InternalKeyInterceptor.java
│   └── WebConfig.java
└── SpeechServiceApplication.java
```

音声変換の契約と実装は `speech`、サービス間認証は `security` に置く。新しい音声機能は原則として `speech` 内へ追加する。

## 5. 命名と追加ルール

- ディレクトリ名とGradle project名はkebab-caseにする。
- Frontend feature名とJava package名は業務用語で統一する。
- 新しい画面は対応する `features/<feature>` に置く。
- 新しいBackend APIは責務を所有するfeature packageに置く。
- サービス間でJava EntityやFrontend Componentを共有しない。
- API契約を共有する必要が出た場合は、Entityではなくschemaから生成する専用packageを追加する。
- サービス固有の環境変数と設定は各サービスの `application.yml` に置く。
- Monorepo全体から実行するコマンドはルート `package.json` に集約する。

## 6. 主なコマンド

```bash
npm run dev
npm test
npm run build
npm run format:check

./gradlew :core-api:test
./gradlew :speech-service:test
npm test --prefix apps/web
```
