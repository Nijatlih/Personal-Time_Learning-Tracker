# SDD · Personal Time & Learning Tracker (PTLT)

## 1. 架构概览

### 1.1 技术栈
- **Backend**: Spring Boot 3.x (Java 21)
- **Database**: PostgreSQL
- **Migration**: Flyway
- **Auth**: JWT (access + refresh token)
- **API**: REST + JSON
- **Build**: Maven

### 1.2 包结构（严格锁定）
```
com.nijat.ptlt
├── auth          # 认证授权
├── workspace     # 工作空间
├── daily         # 日常记录（Phase 1）
├── routine       # 例行事项（Phase 1）
├── task          # 任务（Phase 2）
├── bootcamp      # 训练营（Phase 1）
├── sync          # 同步机制
├── analytics     # 分析（Phase 3）
├── insight       # 洞察（Phase 4）
└── common        # 通用（异常、响应、工具）
```

## 2. Phase 0 设计

### 2.1 核心实体（Phase 0）

#### User
- `id` (UUID, PK)
- `email` (unique, not null)
- `password_hash` (not null)
- `created_at` (timestamptz)
- `updated_at` (timestamptz)
- `deleted_at` (timestamptz, nullable)

#### Workspace
- `id` (UUID, PK)
- `name` (not null)
- `owner_id` (FK → users.id)
- `created_at` (timestamptz)
- `updated_at` (timestamptz)
- `version` (int, default 0) # 乐观锁
- `deleted_at` (timestamptz, nullable)

#### WorkspaceMember
- `id` (UUID, PK)
- `workspace_id` (FK → workspaces.id)
- `user_id` (FK → users.id)
- `role` (enum: OWNER, EDITOR, VIEWER)
- `created_at` (timestamptz)
- `updated_at` (timestamptz)
- `deleted_at` (timestamptz, nullable)
- Unique: (workspace_id, user_id)

### 2.2 Auth 设计

#### JWT Token 策略
- **Access Token**: 短期（15分钟），包含 user_id, workspace_id（当前）
- **Refresh Token**: 长期（7天），存储于数据库，支持轮换
- **Token 存储**: `refresh_tokens` 表

#### API 端点
- `POST /api/v1/auth/register` - 注册
- `POST /api/v1/auth/login` - 登录
- `POST /api/v1/auth/refresh` - 刷新token
- `POST /api/v1/auth/logout` - 登出

### 2.3 Workspace 设计

#### API 端点
- `GET /api/v1/workspaces` - 列表（当前用户）
- `POST /api/v1/workspaces` - 创建
- `GET /api/v1/workspaces/{id}` - 详情
- `PUT /api/v1/workspaces/{id}` - 更新（需校验version）
- `DELETE /api/v1/workspaces/{id}` - 软删除
- `POST /api/v1/workspaces/{id}/invite` - 邀请成员

#### 权限校验
- 所有Service层方法必须校验 `workspace_id` 归属
- Repository 提供 `findByIdAndWorkspaceId` 方法

### 2.4 Sync 基础设计

#### 同步策略
- **Pull**: `POST /api/v1/sync/pull?since={cursor}`
  - 返回 `updatedAt > since` 的所有实体
  - Cursor: ISO 8601 timestamp
  
- **Push**: `POST /api/v1/sync/push`
  - 请求体：实体数组 + version
  - 服务端校验 version，冲突返回 409

#### 冲突处理
- **乐观锁**: version 字段
- **冲突响应**: 409 + server state + diff hints
- **策略**: Last Write Wins + audit log

#### 实体同步字段要求
- `updated_at` (timestamptz, 索引)
- `version` (int, default 0)
- `workspace_id` (所有业务实体)

## 3. 数据一致性规则

### 3.1 多租户隔离
- 所有业务实体必须包含 `workspace_id`
- Service 层必须校验 workspace 归属
- Repository 查询必须过滤 workspace_id

### 3.2 乐观锁
- 所有可更新实体包含 `version`
- 更新时检查 version，不匹配抛出 `ConflictException`
- 更新成功后 version++

## 4. 异常处理

### 4.1 自定义异常
- `NotFoundException` - 资源不存在
- `ConflictException` - 版本冲突（409）
- `ForbiddenException` - 权限不足（403）
- `UnauthorizedException` - 未授权（401）
- `ValidationException` - 验证失败（400）

### 4.2 统一响应
```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Workspace not found",
    "details": {}
  }
}
```

## 5. 安全设计

### 5.1 密码
- 使用 BCrypt（Spring Security 默认）
- 强度要求：至少8位

### 5.2 JWT
- 签名算法：HS256
- Secret：从配置读取（生产环境必须外部化）

### 5.3 权限
- 所有业务操作必须校验 workspace 成员身份
- OWNER: 全部权限
- EDITOR: 读写权限
- VIEWER: 只读权限

## 6. 数据库约定

### 6.1 字段规范
- 主键：UUID (java.util.UUID)
- 时间：timestamptz，默认 now()
- 软删除：deleted_at (nullable)
- 乐观锁：version int default 0
- 多租户：workspace_id (UUID, not null, indexed)

### 6.2 索引策略
- 主键自动索引
- workspace_id 单列索引
- (workspace_id, updated_at) 复合索引（用于sync）
- 唯一约束：按业务需求

## 7. Phase 0 实现清单

- [x] 项目结构
- [ ] User 实体 + Repository
- [ ] Auth Service + Controller
- [ ] JWT 工具类
- [ ] Workspace 实体 + Repository
- [ ] WorkspaceMember 实体 + Repository
- [ ] Workspace Service + Controller
- [ ] Sync Service（基础）
- [ ] 异常体系
- [ ] 统一响应
- [ ] Flyway 迁移脚本
- [ ] 配置（application.yml）

## 8. 非 Phase 0 内容（仅占位）

以下内容在 Phase 0 中**仅做接口/占位设计**，不实现业务逻辑：
- Daily records（Phase 1）
- Routines（Phase 1）
- Tasks（Phase 2）
- Analytics（Phase 3）
- AI/Insights（Phase 4）
