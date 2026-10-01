-- 英会話レベルと語彙ヒントを保存します。Phase 1.5の会話難易度と教材Feedbackに必要な追加スキーマです。

ALTER TABLE users ADD COLUMN english_level VARCHAR(20);
ALTER TABLE conversation_feedbacks ADD COLUMN vocabulary_tips JSON;
