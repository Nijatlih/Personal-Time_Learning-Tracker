-- Phase 2: Tasks & Time Blocking
-- 包含：tasks, task_instances, time_blocks

-- Tasks表
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL CHECK (status IN ('TODO', 'IN_PROGRESS', 'COMPLETED', 'ARCHIVED')),
    is_core_task BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_tasks_workspace_id ON tasks(workspace_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_tasks_status ON tasks(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_tasks_is_core_task ON tasks(is_core_task) WHERE deleted_at IS NULL AND is_core_task = true;
CREATE INDEX idx_tasks_updated_at ON tasks(updated_at) WHERE deleted_at IS NULL;

-- Task Instances表
CREATE TABLE task_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    task_id UUID NOT NULL REFERENCES tasks(id),
    date DATE NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT false,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    UNIQUE(workspace_id, task_id, date)
);

CREATE INDEX idx_task_instances_workspace_id ON task_instances(workspace_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_task_instances_task_id ON task_instances(task_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_task_instances_date ON task_instances(date) WHERE deleted_at IS NULL;
CREATE INDEX idx_task_instances_workspace_date ON task_instances(workspace_id, date) WHERE deleted_at IS NULL;
CREATE INDEX idx_task_instances_updated_at ON task_instances(updated_at) WHERE deleted_at IS NULL;

-- Time Blocks表
CREATE TABLE time_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    task_id UUID REFERENCES tasks(id),
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    title VARCHAR(200),
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_time_blocks_workspace_id ON time_blocks(workspace_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_time_blocks_date ON time_blocks(date) WHERE deleted_at IS NULL;
CREATE INDEX idx_time_blocks_workspace_date ON time_blocks(workspace_id, date) WHERE deleted_at IS NULL;
CREATE INDEX idx_time_blocks_task_id ON time_blocks(task_id) WHERE deleted_at IS NULL AND task_id IS NOT NULL;
CREATE INDEX idx_time_blocks_updated_at ON time_blocks(updated_at) WHERE deleted_at IS NULL;
