-- PTLT schema (PostgreSQL)

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  email varchar(255) NOT NULL UNIQUE,
  password_hash varchar(255) NOT NULL,
  display_name varchar(255),
  timezone varchar(64),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS workspaces (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  name varchar(255) NOT NULL,
  type varchar(20) NOT NULL,
  owner_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS daily_records (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  workspace_id uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  date date NOT NULL,
  cognitive_energy varchar(10),
  aligned_with_goals boolean,
  core_task_completed boolean,
  notes text,
  morning_completion_rate numeric(5,2) NOT NULL DEFAULT 0,
  evening_completion_rate numeric(5,2) NOT NULL DEFAULT 0,
  stability_score int NOT NULL DEFAULT 0,
  daily_status varchar(10) NOT NULL DEFAULT 'GRAY',
  energy_budget_start int NOT NULL DEFAULT 100,
  energy_budget_current int NOT NULL DEFAULT 100,
  energy_budget_consumed int NOT NULL DEFAULT 0,
  version int NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_daily_record UNIQUE (workspace_id, date)
);

CREATE TABLE IF NOT EXISTS routine_definitions (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  workspace_id uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  routine_type varchar(20) NOT NULL,
  item_key varchar(80) NOT NULL,
  label varchar(255) NOT NULL,
  description text,
  sort_order int NOT NULL,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_routine_def UNIQUE (workspace_id, routine_type, item_key)
);

CREATE TABLE IF NOT EXISTS routine_completions (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  daily_record_id uuid NOT NULL REFERENCES daily_records(id) ON DELETE CASCADE,
  routine_definition_id uuid NOT NULL REFERENCES routine_definitions(id) ON DELETE CASCADE,
  completed boolean NOT NULL DEFAULT false,
  completed_at timestamptz,
  notes text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_routine_completion UNIQUE (daily_record_id, routine_definition_id)
);

CREATE TABLE IF NOT EXISTS tasks (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  workspace_id uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  title varchar(255) NOT NULL,
  description text,
  quadrant varchar(2) NOT NULL,
  priority varchar(2),
  energy_required varchar(10),
  task_type varchar(20) NOT NULL DEFAULT 'ONE_OFF',
  estimate_minutes int,
  deadline timestamptz,
  module_ids text,
  tags text,
  status varchar(20) NOT NULL DEFAULT 'TODO',
  completion_percent int NOT NULL DEFAULT 0,
  actual_minutes int,
  completed_at timestamptz,
  version int NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- updated_at trigger
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_users_updated_at') THEN
    CREATE TRIGGER tr_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_workspaces_updated_at') THEN
    CREATE TRIGGER tr_workspaces_updated_at BEFORE UPDATE ON workspaces FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_daily_records_updated_at') THEN
    CREATE TRIGGER tr_daily_records_updated_at BEFORE UPDATE ON daily_records FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_routine_definitions_updated_at') THEN
    CREATE TRIGGER tr_routine_definitions_updated_at BEFORE UPDATE ON routine_definitions FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_routine_completions_updated_at') THEN
    CREATE TRIGGER tr_routine_completions_updated_at BEFORE UPDATE ON routine_completions FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_tasks_updated_at') THEN
    CREATE TRIGGER tr_tasks_updated_at BEFORE UPDATE ON tasks FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

-- -----------------------------------------------------------------
-- Algorithm triggers (from 文档3)
-- -----------------------------------------------------------------

CREATE OR REPLACE FUNCTION calculate_daily_stability()
RETURNS TRIGGER AS $$
DECLARE
    morning_rate DECIMAL := 0;
    evening_rate DECIMAL := 0;
    morning_count INT;
    morning_completed INT;
    evening_count INT;
    evening_completed INT;
    score INT := 0;
    status VARCHAR(10);
BEGIN
    SELECT COUNT(*) FILTER (WHERE rd.active = true),
           COUNT(*) FILTER (WHERE rd.active = true AND rc.completed = true)
      INTO morning_count, morning_completed
      FROM routine_definitions rd
      LEFT JOIN routine_completions rc
        ON rc.routine_definition_id = rd.id AND rc.daily_record_id = NEW.id
     WHERE rd.workspace_id = NEW.workspace_id AND rd.routine_type = 'MORNING';

    IF morning_count > 0 THEN
      morning_rate := (morning_completed::DECIMAL / morning_count::DECIMAL) * 100;
    END IF;

    SELECT COUNT(*) FILTER (WHERE rd.active = true),
           COUNT(*) FILTER (WHERE rd.active = true AND rc.completed = true)
      INTO evening_count, evening_completed
      FROM routine_definitions rd
      LEFT JOIN routine_completions rc
        ON rc.routine_definition_id = rd.id AND rc.daily_record_id = NEW.id
     WHERE rd.workspace_id = NEW.workspace_id AND rd.routine_type = 'EVENING';

    IF evening_count > 0 THEN
      evening_rate := (evening_completed::DECIMAL / evening_count::DECIMAL) * 100;
    END IF;

    score := score + ((morning_rate / 100) * 20)::INT;
    score := score + ((evening_rate / 100) * 20)::INT;

    IF NEW.core_task_completed = true THEN
      score := score + 30;
    END IF;

    IF NEW.cognitive_energy = 'HIGH' THEN
      score := score + 15;
    ELSIF NEW.cognitive_energy = 'MEDIUM' THEN
      score := score + 10;
    END IF;

    IF NEW.aligned_with_goals = true THEN
      score := score + 15;
    END IF;

    IF score >= 80 THEN
      status := 'GREEN';
    ELSIF score >= 50 THEN
      status := 'YELLOW';
    ELSE
      status := 'GRAY';
    END IF;

    NEW.morning_completion_rate := morning_rate;
    NEW.evening_completion_rate := evening_rate;
    NEW.stability_score := score;
    NEW.daily_status := status;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_daily_records_calc_stability') THEN
    CREATE TRIGGER tr_daily_records_calc_stability
      BEFORE INSERT OR UPDATE OF cognitive_energy, aligned_with_goals, core_task_completed
      ON daily_records
      FOR EACH ROW
      EXECUTE FUNCTION calculate_daily_stability();
  END IF;
END $$;

-- When routine completion changes, touch daily_records to recompute
CREATE OR REPLACE FUNCTION touch_daily_record()
RETURNS TRIGGER AS $$
DECLARE
  dr_id uuid;
BEGIN
  dr_id := COALESCE(NEW.daily_record_id, OLD.daily_record_id);
  UPDATE daily_records SET version = version + 1 WHERE id = dr_id;
  RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_routine_completion_touch') THEN
    CREATE TRIGGER tr_routine_completion_touch
      AFTER INSERT OR UPDATE OF completed OR DELETE
      ON routine_completions
      FOR EACH ROW
      EXECUTE FUNCTION touch_daily_record();
  END IF;
END $$;

-- Energy restore on completion (simplified)
CREATE OR REPLACE FUNCTION restore_energy_on_routine_completion()
RETURNS TRIGGER AS $$
DECLARE
  restore_amount INT := 0;
BEGIN
  IF NEW.completed = true THEN
    IF EXISTS (SELECT 1 FROM routine_definitions rd WHERE rd.id = NEW.routine_definition_id AND rd.routine_type = 'MORNING') THEN
      restore_amount := 8;
    ELSIF EXISTS (SELECT 1 FROM routine_definitions rd WHERE rd.id = NEW.routine_definition_id AND rd.routine_type = 'EVENING') THEN
      restore_amount := 5;
    END IF;

    UPDATE daily_records
       SET energy_budget_current = LEAST(100, energy_budget_current + restore_amount)
     WHERE id = NEW.daily_record_id;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_restore_energy') THEN
    CREATE TRIGGER tr_restore_energy
      AFTER INSERT OR UPDATE OF completed
      ON routine_completions
      FOR EACH ROW
      EXECUTE FUNCTION restore_energy_on_routine_completion();
  END IF;
END $$;

CREATE OR REPLACE FUNCTION calculate_daily_stability()
RETURNS TRIGGER AS $$
DECLARE
    morning_count INT;
    morning_completed INT;
    evening_count INT;
    evening_completed INT;
    score INT := 0;
    status VARCHAR(10);
    morning_rate NUMERIC(5,2) := 0;
    evening_rate NUMERIC(5,2) := 0;
BEGIN
    -- Morning completion rate
    SELECT COUNT(*) INTO morning_count
      FROM routine_definitions rd
      WHERE rd.workspace_id = NEW.workspace_id AND rd.routine_type = 'MORNING' AND rd.active = true;

    SELECT COUNT(*) INTO morning_completed
      FROM routine_completions rc
      JOIN routine_definitions rd ON rd.id = rc.routine_definition_id
      WHERE rc.daily_record_id = NEW.id AND rd.routine_type = 'MORNING' AND rc.completed = true;

    IF morning_count > 0 THEN
      morning_rate := ROUND((morning_completed::numeric / morning_count::numeric) * 100, 2);
    END IF;

    -- Evening completion rate
    SELECT COUNT(*) INTO evening_count
      FROM routine_definitions rd
      WHERE rd.workspace_id = NEW.workspace_id AND rd.routine_type = 'EVENING' AND rd.active = true;

    SELECT COUNT(*) INTO evening_completed
      FROM routine_completions rc
      JOIN routine_definitions rd ON rd.id = rc.routine_definition_id
      WHERE rc.daily_record_id = NEW.id AND rd.routine_type = 'EVENING' AND rc.completed = true;

    IF evening_count > 0 THEN
      evening_rate := ROUND((evening_completed::numeric / evening_count::numeric) * 100, 2);
    END IF;

    -- Score: morning 20 + evening 20 + core task 30 + energy 15 + aligned 15
    score := 0;
    score := score + ROUND((morning_rate / 100.0) * 20);
    score := score + ROUND((evening_rate / 100.0) * 20);

    IF NEW.core_task_completed = true THEN
      score := score + 30;
    END IF;

    IF NEW.cognitive_energy = 'HIGH' THEN
      score := score + 15;
    ELSIF NEW.cognitive_energy = 'MEDIUM' THEN
      score := score + 10;
    END IF;

    IF NEW.aligned_with_goals = true THEN
      score := score + 15;
    END IF;

    IF score >= 80 THEN
      status := 'GREEN';
    ELSIF score >= 50 THEN
      status := 'YELLOW';
    ELSE
      status := 'GRAY';
    END IF;

    NEW.morning_completion_rate := morning_rate;
    NEW.evening_completion_rate := evening_rate;
    NEW.stability_score := score;
    NEW.daily_status := status;
    NEW.version := NEW.version + 1;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_daily_records_calc') THEN
    CREATE TRIGGER tr_daily_records_calc BEFORE INSERT OR UPDATE ON daily_records
    FOR EACH ROW EXECUTE FUNCTION calculate_daily_stability();
  END IF;
END $$;

-- Energy restore: when a routine completion is marked completed, restore small energy
CREATE OR REPLACE FUNCTION restore_energy_on_routine_completion()
RETURNS TRIGGER AS $$
DECLARE
  restore_points INT := 0;
  rtype VARCHAR(10);
BEGIN
  IF (TG_OP = 'INSERT' AND NEW.completed = true) OR (TG_OP = 'UPDATE' AND NEW.completed = true AND OLD.completed = false) THEN
    SELECT routine_type INTO rtype FROM routine_definitions WHERE id = NEW.routine_definition_id;
    IF rtype = 'MORNING' THEN
      restore_points := 5;
    ELSIF rtype = 'EVENING' THEN
      restore_points := 3;
    ELSE
      restore_points := 2;
    END IF;

    UPDATE daily_records
      SET energy_budget_current = LEAST(100, energy_budget_current + restore_points)
      WHERE id = NEW.daily_record_id;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_routine_completion_energy') THEN
    CREATE TRIGGER tr_routine_completion_energy AFTER INSERT OR UPDATE ON routine_completions
    FOR EACH ROW EXECUTE FUNCTION restore_energy_on_routine_completion();
  END IF;
END $$;
