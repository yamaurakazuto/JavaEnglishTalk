CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  display_name VARCHAR(50) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);

CREATE TABLE conversation_sessions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL,
  started_at DATETIME(6) NOT NULL,
  finished_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_sessions_user_started (user_id, started_at)
);

CREATE TABLE conversation_messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  content TEXT NOT NULL,
  sequence_no INT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_messages_session FOREIGN KEY (session_id) REFERENCES conversation_sessions(id),
  CONSTRAINT uk_messages_sequence UNIQUE (session_id, sequence_no)
);

CREATE TABLE conversation_feedbacks (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id BIGINT NOT NULL UNIQUE,
  summary TEXT NOT NULL,
  strengths JSON NOT NULL,
  improvements JSON NOT NULL,
  corrections JSON NOT NULL,
  overall_comment TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_feedback_session FOREIGN KEY (session_id) REFERENCES conversation_sessions(id)
);
