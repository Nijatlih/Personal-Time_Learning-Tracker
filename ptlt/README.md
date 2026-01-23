# PTLT (Personal Time & Learning Tracker)

本仓库包含：
- `backend/`：Spring Boot + PostgreSQL + Flyway（JWT 鉴权，Daily Records / Tasks / Auth）
- `frontend/`：React + TypeScript + Vite（页面与 API Client Skeleton）

> 说明：此包是按你发的 Part 2–5 + 文档 2/3 进行补齐的“工程骨架”。由于当前环境无法执行 Maven/NPM 构建验证，可能仍需你本地 `mvn test` / `npm run dev` 进一步校验并微调。

## 运行（开发）

### 1) PostgreSQL
建议用 Docker：
```bash
docker compose up -d db
```

### 2) Backend
```bash
cd backend
mvn spring-boot:run
```

### 3) Frontend
```bash
cd frontend
npm install
npm run dev
```

## API 路由（前端当前依赖）
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/daily-records/{date}?workspaceId=`
- `PUT /api/v1/daily-records/{date}?workspaceId=`
- `GET /api/v1/daily-records?workspaceId=&from=&to=`
- `GET /api/v1/tasks?workspaceId=`
- `POST /api/v1/tasks?workspaceId=`
- `PATCH /api/v1/tasks/{id}`
- `DELETE /api/v1/tasks/{id}`
- `POST /api/v1/tasks/{id}/complete`
