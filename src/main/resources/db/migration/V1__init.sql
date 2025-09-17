CREATE TABLE IF NOT EXISTS app_state (
  id BIGSERIAL PRIMARY KEY,
  installed_at TIMESTAMP NOT NULL,
  last_processed_week_start DATE,
  anna_username VARCHAR(100) NOT NULL,
  anna_balance INTEGER NOT NULL,
  anna_avatar_url VARCHAR(255),
  state_json JSONB NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_app_state_updated_at ON app_state(updated_at);


