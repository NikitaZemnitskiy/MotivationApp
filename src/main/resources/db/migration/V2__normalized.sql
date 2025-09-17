-- App meta
CREATE TABLE IF NOT EXISTS app_meta (
  id BIGSERIAL PRIMARY KEY,
  installed_at TIMESTAMP NOT NULL,
  last_processed_week_start DATE
);

-- Users
CREATE TABLE IF NOT EXISTS app_user (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(100) NOT NULL,
  balance INTEGER NOT NULL DEFAULT 0,
  avatar_url VARCHAR(255)
);

-- Daily task definitions
CREATE TABLE IF NOT EXISTS daily_task_def (
  id VARCHAR(100) PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  kind VARCHAR(20) NOT NULL,
  daily_reward INTEGER NOT NULL,
  minutes_per_day INTEGER,
  weekly_minutes_goal INTEGER,
  streak_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  weekly_required_count INTEGER
);

-- One-time goals
CREATE TABLE IF NOT EXISTS one_time_goal (
  id VARCHAR(100) PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  reward INTEGER NOT NULL,
  completed_at TIMESTAMP,
  user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE
);

-- Shop items
CREATE TABLE IF NOT EXISTS shop_item (
  id VARCHAR(100) PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  cost INTEGER NOT NULL
);

-- Purchases
CREATE TABLE IF NOT EXISTS purchase (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  shop_item_id VARCHAR(100),
  title_snapshot VARCHAR(255) NOT NULL,
  cost_snapshot INTEGER NOT NULL,
  purchased_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_purchase_item FOREIGN KEY (shop_item_id) REFERENCES shop_item(id) ON DELETE SET NULL
);

-- Gifts
CREATE TABLE IF NOT EXISTS gift (
  id VARCHAR(100) PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  title VARCHAR(255) NOT NULL,
  amount INTEGER NOT NULL
);

-- Daily logs (per date per user)
CREATE TABLE IF NOT EXISTS daily_log (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  date DATE NOT NULL,
  CONSTRAINT ux_daily_log_user_date UNIQUE(user_id, date)
);

CREATE TABLE IF NOT EXISTS daily_log_minutes (
  id BIGSERIAL PRIMARY KEY,
  daily_log_id BIGINT NOT NULL REFERENCES daily_log(id) ON DELETE CASCADE,
  task_id VARCHAR(100) NOT NULL REFERENCES daily_task_def(id) ON DELETE CASCADE,
  minutes INTEGER NOT NULL,
  awarded BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT ux_minutes_log_task UNIQUE(daily_log_id, task_id)
);

CREATE TABLE IF NOT EXISTS daily_log_checks (
  id BIGSERIAL PRIMARY KEY,
  daily_log_id BIGINT NOT NULL REFERENCES daily_log(id) ON DELETE CASCADE,
  task_id VARCHAR(100) NOT NULL REFERENCES daily_task_def(id) ON DELETE CASCADE,
  CONSTRAINT ux_checks_log_task UNIQUE(daily_log_id, task_id)
);

-- Streaks per task
CREATE TABLE IF NOT EXISTS streak (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  task_id VARCHAR(100) NOT NULL REFERENCES daily_task_def(id) ON DELETE CASCADE,
  count INTEGER NOT NULL,
  CONSTRAINT ux_streak_user_task UNIQUE(user_id, task_id)
);

-- History extras
CREATE TABLE IF NOT EXISTS history_extra (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  date DATE NOT NULL,
  label VARCHAR(255) NOT NULL,
  points INTEGER NOT NULL
);

-- Roulette state per day
CREATE TABLE IF NOT EXISTS roulette_state (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  date DATE NOT NULL,
  effect VARCHAR(50),
  daily_id VARCHAR(100),
  daily_base_reward INTEGER,
  daily_penalty_applied BOOLEAN DEFAULT FALSE,
  goal_id VARCHAR(100),
  bonus_points INTEGER,
  discounted_shop_id VARCHAR(100),
  free_shop_id VARCHAR(100),
  CONSTRAINT ux_roulette_user_date UNIQUE(user_id, date)
);


