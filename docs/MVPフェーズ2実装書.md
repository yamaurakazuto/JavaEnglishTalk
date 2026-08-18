# TalkOn MVP フェーズ2実装書

## 1. 目的と実装判断

フェーズ2で何を考え、どこへ実装したかを残す作業記録である。最優先を会話品質とし、Realtime APIは使わず、STT、LLM、TTSを独立した境界として追加した。これにより、認識精度、会話品質、音声品質、速度、障害を別々に調査できる。

音声ターンは `STT → 既存ConversationService → TTS` の順に処理する。文字起こし後に既存のテキスト送信処理を再利用するため、音声とテキストで履歴順序、所有者認可、Level制御が分岐しない。

TTSは会話成立後に行う。TTSが失敗しても保存済みの文字起こしとAI英文を返し、画面から音声だけ再試行できる。STTまたはLLMが失敗した場合は段階を識別できるエラーを返す。

ブラウザ音声は永続保存しない。Spring Bootが同じリクエスト内でOpenAIへ転送した後に破棄し、会話履歴には文字起こしとAI英文だけを保存する。ログには音声本体や会話本文を出さず、bytes、文字数、処理時間、モデル名、利用tokenを記録する。

## 2. 実装内容

### 会話品質

- 直前の発言への反応、質問の重複回避、1〜3文、質問最大1件をPromptへ追加した。
- 意味が不明な場合だけ短く聞き返し、軽微な文法ミスは会話中に講義しない。
- BEGINNER、INTERMEDIATE、ADVANCEDのLevel Policyを維持した。
- 履歴上限を `LLM_HISTORY_LIMIT` で設定可能にした。
- `ConversationAIService` を会話生成専用のPortとし、翻訳・Feedbackから分離した。

### STT・TTS・UI

- ReactのMediaRecorderで最大60秒録音し、multipartでSpring Bootへ送る。
- `SpeechRecognitionService` と `TextToSpeechService` がOpenAI固有形式を隠す。
- 512 bytes未満、5 MiB超過、非対応Content-Type、外部API失敗を区別する。
- 文字起こし結果をユーザーメッセージとして保存し、画面にも表示する。
- 初期TTSは `gpt-4o-mini-tts`、voice `coral`、speed `1.0` とする。
- 音声ターン成功時はReactが自動再生を試し、拒否時は再生controlsを表示する。
- 各AIメッセージの「英語を聞く」から音声だけ再生成できる。
- APIキーが空の通常開発・テストではFake STT/TTS/LLMを使い、課金しない。

## 3. API仕様

### 音声会話

```http
POST /api/conversations/{conversationId}/voice-turns
Content-Type: multipart/form-data

audio: recording.webm
```

```json
{
  "userTranscript": "I play soccer every Sunday.",
  "conversation": { "id": 1, "messages": [] },
  "assistantAudioBase64": "...",
  "audioContentType": "audio/mpeg",
  "processingTimes": {
    "sttMs": 850,
    "llmMs": 620,
    "ttsMs": 740,
    "totalMs": 2210
  },
  "warning": null
}
```

TTSだけ失敗した場合は `conversation` と `userTranscript` を返し、音声項目をnull、`warning` を失敗理由とする。

### 音声再生成

```http
POST /api/conversations/{conversationId}/messages/{messageId}/speech
```

所有する会話のASSISTANTメッセージだけを対象に音声binaryを返す。クライアントから自由な英文を受け取らない。

| HTTP | code                     | 意味                       |
| ---: | ------------------------ | -------------------------- |
|  400 | `AUDIO_TOO_SHORT`        | 短すぎる音声または無音候補 |
|  413 | `AUDIO_TOO_LARGE`        | 5 MiB超過                  |
|  415 | `UNSUPPORTED_AUDIO_TYPE` | 非対応Content-Type         |
|  503 | `STT_UNAVAILABLE`        | STT失敗                    |
|  503 | `LLM_UNAVAILABLE`        | LLM失敗                    |
|  503 | `TTS_UNAVAILABLE`        | 音声再生成時のTTS失敗      |

## 4. 環境変数

| 環境変数                       | 初期値                   | 用途               |
| ------------------------------ | ------------------------ | ------------------ |
| `OPENAI_API_KEY`               | 空                       | 空ならFake実装     |
| `OPENAI_BASE_URL`              | OpenAI v1 API            | API基点            |
| `OPENAI_MODEL`                 | `gpt-4.1-mini`           | 会話LLM            |
| `LLM_HISTORY_LIMIT`            | `20`                     | 履歴メッセージ上限 |
| `OPENAI_STT_MODEL`             | `gpt-4o-mini-transcribe` | STT候補            |
| `OPENAI_TTS_MODEL`             | `gpt-4o-mini-tts`        | TTS                |
| `OPENAI_TTS_VOICE`             | `coral`                  | voice              |
| `OPENAI_TTS_SPEED`             | `1.0`                    | 速度               |
| `OPENAI_TTS_FORMAT`            | `mp3`                    | 出力形式           |
| `SPEECH_TIMEOUT_SECONDS`       | `30`                     | timeout            |
| `SPEECH_RETRY_COUNT`           | `1`                      | 追加試行回数       |
| `SPEECH_MAX_BYTES`             | `5242880`                | 音声上限           |
| `SPEECH_MAX_RECORDING_SECONDS` | `60`                     | 録音時間の設計上限 |

フロントエンドもMVPでは60秒で停止する。設定配信APIは追加していないため、変更時は双方を合わせる。

## 5. モデル比較とコスト計画

2026年8月11日にOpenAI公式の[Speech to text](https://developers.openai.com/api/docs/guides/speech-to-text)、[Text to speech](https://developers.openai.com/api/docs/guides/text-to-speech)、[API Pricing](https://openai.com/api/pricing/)を確認した。

| 領域 | 候補                                        | 初期判断                         | 実API比較          |
| ---- | ------------------------------------------- | -------------------------------- | ------------------ |
| STT  | `gpt-4o-mini-transcribe` / `gpt-transcribe` | miniを低コスト候補として設定化   | 日本人英語は未実測 |
| LLM  | `gpt-4.1-mini`ほか                          | 既存採用を維持して評価条件を固定 | 未実測             |
| TTS  | `gpt-4o-mini-tts`                           | 指示書の第一候補                 | voice比較は未実測  |

計画値としてSTT約$0.003/分、TTS約$0.015/分、LLM input $0.40/100万token、output $1.60/100万tokenを使う。月225分ずつ、月600ターン、平均input 1,200 token、output 60 token、1ドル160円で試算する。

| 区分       |         月額概算 |
| ---------- | ---------------: |
| STT        |           $0.675 |
| TTS        |           $3.375 |
| LLM input  |           $0.288 |
| LLM output |           $0.058 |
| 合計       | 約$4.40、約704円 |

1,500円まで約796円の余裕がある。ただし実測ではない。料金、会話比率、履歴長、為替で変わるため、本番モデル確定時に公式料金と構造化ログで再計算する。

## 6. 作業ファイル

| ファイル                                                                          | 実装内容                   |
| --------------------------------------------------------------------------------- | -------------------------- |
| `backend/src/main/java/com/talkon/TalkOnApplication.java`                  | 設定型scan                 |
| `backend/src/main/java/com/talkon/common/ApiExceptionHandler.java`         | multipart上限エラーの統一  |
| `backend/src/main/java/com/talkon/conversation/ConversationAIService.java` | 会話生成Port               |
| `backend/src/main/java/com/talkon/conversation/TranslationService.java`    | 翻訳専用Port               |
| `backend/src/main/java/com/talkon/conversation/ConversationService.java`   | Portへの依存               |
| `backend/src/main/java/com/talkon/llm/ConversationAiClient.java`           | adapter契約                |
| `backend/src/main/java/com/talkon/llm/AiClientConfig.java`                 | 履歴上限、token、model計測 |
| `backend/src/main/java/com/talkon/llm/Prompts.java`                        | 会話品質規則               |
| `backend/src/main/java/com/talkon/speech/SpeechRecognitionService.java`    | STT Port                   |
| `backend/src/main/java/com/talkon/speech/TextToSpeechService.java`         | TTS Port                   |
| `backend/src/main/java/com/talkon/speech/SpeechProperties.java`            | 音声設定                   |
| `backend/src/main/java/com/talkon/speech/SpeechClientConfig.java`          | OpenAI/Fake adapter        |
| `backend/src/main/java/com/talkon/speech/VoiceConversationService.java`    | 音声ターン、検証、計測     |
| `backend/src/main/java/com/talkon/speech/VoiceConversationController.java` | multipart・音声API         |
| `backend/src/main/resources/application.yml`                                      | モデル・上限設定           |
| `backend/src/test/resources/application.yml`                                      | 非課金test設定             |
| `backend/src/test/java/com/talkon/ConversationIntegrationTest.java`        | 音声結合テスト             |
| `backend/src/test/java/com/talkon/VoiceConversationServiceTest.java`       | TTS部分失敗テスト          |
| `frontend/src/shared/api.ts`                                                      | multipart・音声取得・型    |
| `frontend/src/features/conversation/VoiceRecorder.tsx`                            | 録音・認識表示・再生       |
| `frontend/src/features/conversation/MessageList.tsx`                              | AI音声ボタン               |
| `frontend/src/App.tsx`                                                            | 録音UI統合                 |
| `frontend/src/styles.css`                                                         | 音声UI                     |
| `docs/MVPフェーズ2実装書.md`                                                      | 本記録                     |
| `docs/CODE_READING_GUIDE.md`                                                      | 音声コードの追い方         |

## 7. テスト、未解決事項、次候補

自動テストではFake STT → LLM → Fake TTS、入力エラー、TTS部分失敗、音声再生成、既存認証・Dashboard・翻訳・Feedbackを確認する。実APIテストは課金防止のため通常テストへ含めない。

- 実音声と実APIを使っていないため、5秒目標、10秒許容、認識精度、失敗率は未計測である。
- 音声は一括アップロードで、ストリーミングや割り込み発話はない。
- MediaRecorderはwebmまたはmp4を選ぶがブラウザ差がある。サーバーは公式仕様に合わせ、mp3、mp4、m4a、wav、webmを受け付ける。
- サーバーはファイルheaderや実durationを解析せず、Content-Type、bytes、フロント60秒で制限する。
- TTS音声は保存しないため、再生成頻度を実測後に期限付きcacheを検討する。
- Realtime APIは分離構成が継続的に10秒を超えた場合だけ比較する。

## 8. 会話単位のToken・料金表示

OpenAIのChat Completionsレスポンスに含まれる `prompt_tokens` と `completion_tokens` を、会話開始と通常返信ごとに取得する。`ConversationSession`へ入力Token、出力Token、概算料金を累積し、会話終了後のFeedback末尾に表示する。

会話中も利用量を把握できるよう、会話画面上部の「会話を終了」の横へ、合計Tokenと概算円額を小さく表示する。AI返信後に返る最新のConversationレスポンスで更新するため、追加の料金取得APIやpollingは行わない。

翻訳と終了後Feedbackは「英会話中に使った料金」と区別するため、この集計には含めない。Fake LLMはTokenを0として返し、「料金は発生していない」と画面に明示する。

概算料金は次の設定値を使用する。

| 環境変数                     | 初期値 | 内容                      |
| ---------------------------- | -----: | ------------------------- |
| `LLM_INPUT_USD_PER_MILLION`  | `0.40` | 入力100万TokenあたりのUSD |
| `LLM_OUTPUT_USD_PER_MILLION` | `1.60` | 出力100万TokenあたりのUSD |
| `LLM_YEN_PER_USD`            |  `160` | 円換算レート              |

料金改定と為替変動があるため、画面上では確定請求額ではなく概算として表示する。モデルを変更した場合は、モデルと同時に単価設定も変更する。

追加・変更した主なファイルは次のとおり。

- `ConversationAIService.java`：AI英文とToken利用量を返す `AiResponse`
- `AiClientConfig.java`：OpenAI usageの取得、Fake利用量0
- `LlmCostCalculator.java`：Token単価と為替による概算料金計算
- `ConversationSession.java`：会話単位の累積利用量
- `V4__add_conversation_llm_usage.sql`：Token、料金、モデル列
- `ConversationDtos.java`、`frontend/src/shared/api.ts`：利用量のAPI契約
- `FeedbackPanel.tsx`：Token、料金、ホームボタン表示
- `LlmCostCalculatorTest.java`：料金単位のテスト
