-- Phase 1: Daily Tracking MVP
-- 包含：routine_definitions, routine_completions, daily_records

-- Routine Definitions表
CREATE TABLE routine_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('MORNING', 'EVENING')),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    UNIQUE(workspace_id, type)
);

CREATE INDEX idx_routine_definitions_workspace_id ON routine_definitions(workspace_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_routine_definitions_type ON routine_definitions(type) WHERE deleted_at IS NULL;
CREATE INDEX idx_routine_definitions_updated_at ON routine_definitions(updated_at) WHERE deleted_at IS NULL;

-- Routine Completions表
CREATE TABLE routine_completions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    routine_definition_id UUID NOT NULL REFERENCES routine_definitions(id),
    date DATE NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT false,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    UNIQUE(workspace_id, date, routine_definition_id)
);

CREATE INDEX idx_routine_completions_workspace_id ON routine_completions(workspace_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_routine_completions_routine_definition_id ON routine_completions(routine_definition_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_routine_completions_date ON routine_completions(date) WHERE deleted_at IS NULL;
CREATE INDEX idx_routine_completions_workspace_date ON routine_completions(workspace_id, date) WHERE deleted_at IS NULL;
CREATE INDEX idx_routine_completions_updated_at ON routine_completions(updated_at) WHERE deleted_at IS NULL;

-- Daily Records表
CREATE TABLE daily_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    date DATE NOT NULL,
    stability_score INTEGER CHECK (stability_score >= 0 AND stability_score <= 100),
    stability_status VARCHAR(20) CHECK (stability_status IN ('GREEN', 'YELLOW', 'GRAY')),
    morning_routine_completed BOOLEAN,
    evening_routine_completed BOOLEAN,
    core_task_completed BOOLEAN,
    cognitive_energy VARCHAR(20) CHECK (cognitive_energy IN ('LOW', 'MEDIUM', 'HIGH')),
    aligned_with_goals BOOLEAN,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    UNIQUE(workspace_id, date)
);

CREATE INDEX idx_daily_records_workspace_id ON daily_records(workspace_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_daily_records_date ON daily_records(date) WHERE deleted_at IS NULL;
CREATE INDEX idx_daily_records_workspace_date ON daily_records(workspace_id, date) WHERE deleted_at IS NULL;
CREATE INDEX idx_daily_records_updated_at ON daily_records(updated_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_daily_records_stability_status ON daily_records(stability_status) WHERE deleted_at IS NULL;
