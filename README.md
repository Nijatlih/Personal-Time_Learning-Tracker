# PTLT — Personal Time & Learning Tracker

PTLT 是一个自用的个人时间、学习与系统稳定度追踪工具。

- **后端：** Spring Boot 3.5.16、Java 25、PostgreSQL、Flyway、JWT
- **前端：** React 19、Vite 8、纯 JavaScript
- **定位：** 单用户、单 workspace；不做多人协作、离线同步、计费或 AI 洞察
- **规模：** 43 个 API 端点、14 张表、6 个 Flyway 迁移、37 个自动化测试

后端只提供 API，不托管前端静态文件。开发时需要分别启动后端和前端。

---

## 功能

当前前端提供：

- 注册与登录
- Dashboard
- Daily Log（日常记录与稳定度评分）
- Tasks
- Routines 与打卡
- Schedule（今天 / 本周的时间块，以及每周固定模板；Time Block 与 Schedule Slot 的前端）
- Analytics
- 中英文界面切换（默认英文，右上角按钮切换，选择保存在浏览器 `localStorage` 的 `lang`）

后端另外提供 Bootcamp、Alert、Workspace 等 API，暂时没有对应的前端页面。

---

## 运行要求

- **JDK 25**
- **Maven 3.8+**
- **Node.js 20.19+** 与 npm
- **PostgreSQL 13+**
- 可选：Docker，用于快速启动 PostgreSQL；运行完整测试时必须有可用的 Docker daemon

检查本机版本：

```bash
java -version
mvn -version
node --version
npm --version
```

---

## 本地启动（推荐方式）

下面使用 Docker 在本机的 `55432` 端口启动 PostgreSQL，避免与已有的 PostgreSQL 冲突。

### 1. 启动 PostgreSQL

先启动 Docker Desktop，并等到界面显示 Docker Engine 正在运行。

**先检查 `ptlt-pg` 容器是否已经存在：**

```powershell
docker ps -a --filter "name=ptlt-pg"
```

- 如果结果中已经有 `ptlt-pg`，执行下面的启动命令。容器已经运行时再次执行也没有问题：

  ```powershell
  docker start ptlt-pg
  ```

- 只有查询结果为空时，才执行一次创建命令：

  ```powershell
  docker run -d --name ptlt-pg -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=ptltdev -e POSTGRES_DB=ptlt -p 55432:5432 postgres:16-alpine
  ```

不要对已经存在的容器重复执行 `docker run --name ptlt-pg`，否则 Docker 会正确地报
`Conflict: the container name "/ptlt-pg" is already in use`。此时不是数据库坏了，改用
`docker start ptlt-pg` 即可。

检查数据库是否就绪：

```powershell
docker ps --filter "name=ptlt-pg"
docker exec ptlt-pg pg_isready -U postgres -d ptlt
```

第二条命令应输出 `accepting connections`。

> 删除 `ptlt-pg` 容器会删除其中的本地 PTLT 数据，请勿在需要保留数据时执行 `docker rm`。

### 2. 设置后端环境变量

后端没有默认数据库凭据，也不提供默认 JWT 密钥。以下命令请在 **PowerShell** 中执行，
并在**同一个 PowerShell 窗口**中启动后端：

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:55432/ptlt"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "ptltdev"
$env:JWT_SECRET = ([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N'))
```

可用下面的命令确认变量已经设置；它只显示长度，不泄露密钥：

```powershell
$env:SPRING_DATASOURCE_URL
$env:DB_USERNAME
$env:JWT_SECRET.Length
```

最后一行应输出 `64`。PowerShell 环境变量只对当前窗口及它启动的进程有效，换一个窗口运行
后端时需要重新设置。

macOS / Linux / Git Bash 对应写法：

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:55432/ptlt'
export DB_USERNAME='postgres'
export DB_PASSWORD='ptltdev'
export JWT_SECRET="$(openssl rand -hex 32)"
```

`JWT_SECRET` 必须至少 32 字节。重启后更换密钥会让浏览器中已有的登录 token 失效，
但不会影响账号和业务数据。

### 3. 启动后端

继续使用刚才设置环境变量的**同一个 PowerShell 窗口**。先进入项目根目录，再启动：

```powershell
cd <项目根目录>
mvn spring-boot:run
```

看到日志中的 `Started PtltApplication` 才表示启动成功。这个窗口需要保持运行，不要在其中继续输入
前端命令。首次启动时 Flyway 会自动创建和迁移数据库。后端地址：

```text
http://localhost:8080
```

后端只提供 `/api/v1/**` API，因此直接打开 `http://localhost:8080` 不会显示前端页面。

### 4. 启动前端

另开一个 **PowerShell** 窗口，不要关闭正在运行后端的窗口：

```powershell
cd <项目根目录>\frontend
npm ci
npm run dev
```

`npm ci` 安装完成后才执行 `npm run dev`。看到 `Local: http://localhost:5173/` 后保持该窗口运行。
浏览器打开：

```text
http://localhost:5173
```

Vite 已把 `/api` 请求代理到 `http://127.0.0.1:8080`。

---

## 登录账号和密码

**项目没有内置账号，也没有默认账号密码。**

第一次使用空数据库时：

1. 打开 `http://localhost:5173/register`
2. 输入你自己的邮箱；本地测试可以使用 `me@example.com`
3. 设置至少 8 位的密码，例如仅用于本地测试的 `Test1234!`
4. Workspace Name 可填写 `Personal`
5. 注册成功后会自动登录

这组邮箱和密码就是以后登录时使用的账号密码。密码以 bcrypt 摘要保存，无法从数据库中读取明文。

为了保护单人实例：

- 数据库中没有用户时，系统自动允许注册第一个账号。
- 第一个账号创建后，注册默认关闭。
- 如确实需要临时创建其他测试账号，请在启动后端前设置：

```powershell
$env:APP_REGISTRATION_ENABLED = "true"
```

或：

```bash
export APP_REGISTRATION_ENABLED=true
```

不要在公开部署时长期打开注册。

### 想从完全干净的本地数据库重新测试怎么办？

系统没有密码找回功能。如果容器里只是可丢弃的测试数据，可以在 PowerShell 中执行：

```powershell
docker rm -f ptlt-pg
docker run -d --name ptlt-pg -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=ptltdev -e POSTGRES_DB=ptlt -p 55432:5432 postgres:16-alpine
docker exec ptlt-pg pg_isready -U postgres -d ptlt
```

等最后一条输出 `accepting connections`，再启动后端并注册第一个账号。

**第一条命令会永久删除该容器内的全部 PTLT 数据。确认没有需要保留的数据后才能执行。**

---

## 不使用 Docker 启动数据库

也可以使用本机已有的 PostgreSQL 13+：

```sql
CREATE DATABASE ptlt;
```

然后按实际地址和凭据设置：

```text
SPRING_DATASOURCE_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
```

默认数据库地址是 `jdbc:postgresql://localhost:5432/ptlt`，但用户名、密码和 JWT 密钥始终必须显式提供。

---

## 构建与测试

### 后端完整验证

```bash
mvn -B clean verify
```

完整验证包含 Testcontainers 集成测试，会启动真实的 `postgres:16-alpine`，因此必须先启动 Docker Desktop。

### 前端验证

```bash
cd frontend
npm ci
npm run lint
npm run build
```

生产构建输出到 `frontend/dist/`，需要独立部署为静态站点；Spring Boot 不会托管它。

---

## 常见问题

### 后端提示 `JWT_SECRET must be set`

当前终端没有设置 `JWT_SECRET`，请重新执行环境变量步骤。环境变量通常只在当前终端会话中有效。

### 后端无法连接数据库

确认：

```bash
docker ps
docker logs ptlt-pg
```

并检查数据库 URL 是否使用了示例中的 `55432` 端口。

### 页面提示无法连接服务器

确认后端窗口仍在运行、日志里出现过 `Started PtltApplication`，并且前端是通过
`http://localhost:5173` 打开的。

### `npm ci` 报 `EPERM ... rolldown-binding...node`

通常是另一个 `npm run dev`/Vite 进程正在使用 `node_modules`。先在运行前端的窗口按 `Ctrl+C`，
确认开发服务器已经停止，再执行 `npm ci`。正常的日常启动不需要反复执行 `npm ci`；只有首次安装
或 `package-lock.json` 变化后才需要。

### 注册返回 `Registration is closed on this instance`

数据库里已经有账号。请使用原账号登录；只有确实需要新建测试账号时才临时设置 `APP_REGISTRATION_ENABLED=true`。

### 完整测试提示找不到 Docker

启动 Docker Desktop后再执行 `mvn clean verify`。不要把集成测试改成 H2；迁移和 schema 契约依赖真实 PostgreSQL。

---

## 关键约定

- 6 个 `PUT` 更新端点必须携带当前 `version`；缺失返回 400，版本冲突返回 409。
- `POST /api/v1/daily-records` 是按 workspace + date 的 create-or-update，不需要 `version`。
- 认证失败返回 401，鉴权失败返回 403。
- 不属于当前 workspace 的资源统一返回 404，避免泄露资源是否存在。
- 日期区间必须正序，跨度不超过 366 天。
- 前端界面文案放在 `frontend/src/i18n/en.js` 和 `zh.js`，两个文件的键必须一致；新增文案要两边同时加，页面里用 `useLanguage()` 的 `t('键')` 取。后端返回的错误信息不翻译。

---

## 项目文档

- [`CLAUDE.md`](CLAUDE.md)：项目约定与当前状态
- [`docs/NEXT_STEPS.md`](docs/NEXT_STEPS.md)：已完成事项与历史决策
- [`docs/VISION_ROADMAP.md`](docs/VISION_ROADMAP.md)：从日程与学习计划吸收经验的长期路线图
- [`docs/AUDIT_2026_08_10.md`](docs/AUDIT_2026_08_10.md)：第二轮审计与实测证据
- [`adr/0001-personal-tool-not-a-product.md`](adr/0001-personal-tool-not-a-product.md)：为什么定位为自用工具
- [`adr/0003-drop-invite-and-role-based-access.md`](adr/0003-drop-invite-and-role-based-access.md)：删除 invite/RBAC
- [`adr/0004-drop-offline-sync.md`](adr/0004-drop-offline-sync.md)：删除离线同步
- [`adr/0005-single-workspace-per-user.md`](adr/0005-single-workspace-per-user.md)：单用户、单 workspace

---

## License

[MIT](LICENSE)
