-- 会話中のLLM利用トークンと概算料金を保存します。会話単位で利用量を画面表示するための追加スキーマです。

ALTER TABLE conversation_sessions ADD COLUMN llm_input_tokens BIGINT NOT NULL DEFAULT 0;
ALTER TABLE conversation_sessions ADD COLUMN llm_output_tokens BIGINT NOT NULL DEFAULT 0;
ALTER TABLE conversation_sessions ADD COLUMN llm_cost_micros BIGINT NOT NULL DEFAULT 0;
ALTER TABLE conversation_sessions ADD COLUMN llm_model VARCHAR(100);
