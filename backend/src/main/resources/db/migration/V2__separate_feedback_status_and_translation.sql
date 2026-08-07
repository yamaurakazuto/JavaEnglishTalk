-- フィードバック生成状態と任意翻訳を保存します。会話終了をAI処理から分離するための追加スキーマです。

ALTER TABLE conversation_feedbacks ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED';
ALTER TABLE conversation_feedbacks ADD COLUMN error_message TEXT;
ALTER TABLE conversation_feedbacks ALTER COLUMN summary DROP NOT NULL;
ALTER TABLE conversation_feedbacks ALTER COLUMN strengths DROP NOT NULL;
ALTER TABLE conversation_feedbacks ALTER COLUMN improvements DROP NOT NULL;
ALTER TABLE conversation_feedbacks ALTER COLUMN corrections DROP NOT NULL;
ALTER TABLE conversation_feedbacks ALTER COLUMN overall_comment DROP NOT NULL;
ALTER TABLE conversation_messages ADD COLUMN translation TEXT;

UPDATE conversation_sessions
SET status = 'ENDED', finished_at = COALESCE(finished_at, updated_at)
WHERE status IN ('FEEDBACK_GENERATING', 'COMPLETED');
