-- Phase 1 补充: Bootcamp、Alerts、Schedule Slots

-- Bootcamp Modules表（系统级静态数据）
CREATE TABLE bootcamp_modules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    order_index INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bootcamp_modules_code ON bootcamp_modules(code);
CREATE INDEX idx_bootcamp_modules_order_index ON bootcamp_modules(order_index);

-- Daily Module Entries表
CREATE TABLE daily_module_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    module_id UUID NOT NULL REFERENCES bootcamp_modules(id),
    date DATE NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT false,
    notes VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    UNIQUE(workspace_id, date, module_id)
);

CREATE INDEX idx_daily_module_entries_workspace_id ON daily_module_entries(workspace_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_daily_module_entries_module_id ON daily_module_entries(module_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_daily_module_entries_date ON daily_module_entries(date) WHERE deleted_at IS NULL;
CREATE INDEX idx_daily_module_entries_workspace_date ON daily_module_entries(workspace_id, date) WHERE deleted_at IS NULL;
CREATE INDEX idx_daily_module_entries_updated_at ON daily_module_entries(updated_at) WHERE deleted_at IS NULL;

-- Alerts表
CREATE TABLE alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('GRAY_ALERT')),
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_alerts_workspace_id ON alerts(workspace_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_alerts_is_read ON alerts(is_read) WHERE deleted_at IS NULL AND is_read = false;
CREATE INDEX idx_alerts_type ON alerts(type) WHERE deleted_at IS NULL;
CREATE INDEX idx_alerts_created_at ON alerts(created_at) WHERE deleted_at IS NULL;

-- Schedule Slots表
CREATE TABLE schedule_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    day_of_week VARCHAR(10) NOT NULL CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    title VARCHAR(200),
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_schedule_slots_workspace_id ON schedule_slots(workspace_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_schedule_slots_day_of_week ON schedule_slots(day_of_week) WHERE deleted_at IS NULL;
CREATE INDEX idx_schedule_slots_updated_at ON schedule_slots(updated_at) WHERE deleted_at IS NULL;

-- 插入Bootcamp模块种子数据（M1-M7）
INSERT INTO bootcamp_modules (code, name, description, order_index) VALUES
('M1', 'Module 1', 'Bootcamp Module 1', 1),
('M2', 'Module 2', 'Bootcamp Module 2', 2),
('M3', 'Module 3', 'Bootcamp Module 3', 3),
('M4', 'Module 4', 'Bootcamp Module 4', 4),
('M5', 'Module 5', 'Bootcamp Module 5', 5),
('M6', 'Module 6', 'Bootcamp Module 6', 6),
('M7', 'Module 7', 'Bootcamp Module 7', 7);
