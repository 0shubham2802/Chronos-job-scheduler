[README.md](https://github.com/user-attachments/files/27559382/README.md)
# ⏱️ Chronos — Job Scheduler

<div align="center">

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React_18-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

**A production-grade distributed job scheduling platform with a modern React frontend, Redis-backed execution queue, and email notifications.**

[Features](#-features) · [Architecture](#-architecture) · [Docker Setup](#-docker-setup) · [Local Setup](#-local-development) · [API Docs](#-api-reference)

</div>

---

## ✨ Features

| Feature | Description |
|---|---|
| 🕐 **One-time Jobs** | Schedule a job to run once at a specific date and time |
| 🔁 **Recurring Jobs** | Use cron expressions for repeating schedules |
| ▶️ **Manual Trigger** | Run any job immediately on demand |
| ⏸️ **Pause / Resume** | Pause recurring jobs and resume them later |
| 🔄 **Exponential Backoff Retry** | Auto-retry failed jobs: 1s → 2s → 4s → 8s → permanent failure |
| 📧 **Email Jobs** | Schedule emails to be sent at a specific time |
| 🔔 **Failure Notifications** | Email or webhook alert when a job permanently fails |
| 📊 **Execution Logs** | Full run history — attempt, status, duration, errors |
| 🔐 **JWT Authentication** | Secure login/register with JWT tokens |
| 🚦 **Distributed Locking** | Redis locks prevent duplicate job execution |
| 🐳 **Docker Support** | Full stack containerised with Docker Compose |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Network                        │
│                                                          │
│  ┌──────────┐     ┌──────────┐     ┌──────────────────┐ │
│  │  React   │────►│  Nginx   │────►│  Spring Boot     │ │
│  │ Frontend │     │ :80      │     │  Backend :8080   │ │
│  └──────────┘     └──────────┘     └────────┬─────────┘ │
│                                             │           │
│                    ┌────────────────────────┼──────────┐│
│                    │                        │          ││
│          ┌─────────▼──┐          ┌──────────▼───────┐  ││
│          │ PostgreSQL │          │     Redis        │  ││
│          │   :5432    │          │     :6379        │  ││
│          └────────────┘          └──────────────────┘  ││
└────────────────────────────────────────────────────────┘│
```

**Execution Flow:**
```
Frontend → POST /api/jobs → Quartz (schedules trigger)
                                   │
                         At scheduled time
                                   │
                         ChronosJobExecutor (Quartz fires)
                                   │
                         JobQueueService → Redis Queue → Thread Pool
                                                              │
                                                    JobExecutionService
                                                    ├── Send email
                                                    ├── Default simulate
                                                    └── Failure → RetryService
                                                                   └── NotificationService
```

---

## 🐳 Docker Setup

> **Run the full stack with a single command.**

### Prerequisites

Install [Docker Desktop](https://www.docker.com/products/docker-desktop/) — that's all you need.

### 1. Clone the repository

```bash
git clone https://github.com/0shubham2802/Chronos-job-scheduler.git
cd Chronos-job-scheduler
```

### 2. Configure environment variables

```bash
cp .env.example .env
```

Edit `.env`:
```env
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your16charapppassword
NOTIFICATION_ENABLED=true
```

> **Gmail App Password:** Go to [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords) → Create app password named "Chronos" → copy the 16-character code.

### 3. Run everything

```bash
docker compose up --build
```

| Container | Service | URL |
|-----------|---------|-----|
| `chronos-postgres` | PostgreSQL | `localhost:5432` |
| `chronos-redis` | Redis | `localhost:6379` |
| `chronos-backend` | Spring Boot API | `http://localhost:8080` |
| `chronos-frontend` | React App | `http://localhost` |

### 4. Open the app

```
http://localhost
```

Register an account and start scheduling jobs!

### Docker Commands

```bash
# Run in background
docker compose up -d --build

# View logs
docker compose logs -f backend
docker compose logs -f frontend

# Stop everything
docker compose down

# Stop and wipe all data
docker compose down -v

# Rebuild one service
docker compose up --build backend

# Shell into backend
docker exec -it chronos-backend sh

# Connect to database
docker exec -it chronos-postgres psql -U postgres -d chronos
```

---

## 💻 Local Development

### Prerequisites

```bash
java --version      # Java 17+
psql --version      # PostgreSQL 14+
redis-cli ping      # Should print PONG
node --version      # Node.js 18+
```

### 1. Create the database

```bash
psql -U postgres -c "CREATE DATABASE chronos;"
```

### 2. Configure `src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/chronos
spring.datasource.username=postgres
spring.datasource.password=postgres

spring.mail.username=your@gmail.com
spring.mail.password=your_app_password
chronos.notification.enabled=true

spring.quartz.properties.org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
spring.quartz.properties.org.quartz.jobStore.isClustered=false
```

### 3. Start Redis

```bash
brew services start redis    # macOS
redis-server                 # Linux
```

### 4. Start the backend

```bash
./gradlew bootRun -x test
# → http://localhost:8080
```

### 5. Start the frontend

```bash
cd chronos-frontend
npm install
npm run dev
# → http://localhost:5173
```

---

## 📋 Job Payload Examples

### Schedule an Email

```json
{
  "name": "Weekly Report",
  "type": "RECURRING",
  "cronExpression": "0 9 * * MON",
  "timezone": "Asia/Kolkata",
  "maxRetries": 3,
  "payload": {
    "to": "recipient@gmail.com",
    "subject": "Weekly Report",
    "body": "Hi, here is your weekly summary."
  }
}
```

### Test Retry Behaviour

```json
{
  "name": "Fail Test",
  "type": "ONE_TIME",
  "scheduledAt": "2026-05-10T10:00:00",
  "timezone": "UTC",
  "maxRetries": 3,
  "payload": { "fail": true }
}
```

---

## 📡 API Reference

### Auth

| Method | Endpoint | Body |
|--------|----------|------|
| `POST` | `/api/auth/register` | `{ name, email, password }` |
| `POST` | `/api/auth/login` | `{ email, password }` |
| `POST` | `/api/auth/refresh` | — |

### Jobs (requires `Authorization: Bearer <token>`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/jobs` | List all jobs |
| `POST` | `/api/jobs` | Create a job |
| `GET` | `/api/jobs/:id` | Get job details |
| `DELETE` | `/api/jobs/:id` | Delete a job |
| `POST` | `/api/jobs/:id/trigger` | Run immediately |
| `POST` | `/api/jobs/:id/pause` | Pause job |
| `POST` | `/api/jobs/:id/resume` | Resume job |
| `GET` | `/api/jobs/:id/logs` | Execution history |

---

## ⚙️ Cron Expression Guide

```
┌───── minute (0-59)
│ ┌───── hour (0-23)
│ │ ┌───── day of month (1-31)
│ │ │ ┌───── month (1-12)
│ │ │ │ ┌───── weekday (SUN-SAT)
* * * * *
```

| Expression | Schedule |
|------------|----------|
| `* * * * *` | Every minute |
| `0 9 * * *` | Every day at 9am |
| `0 9 * * MON` | Every Monday 9am |
| `0 0 1 * *` | First of every month |
| `*/15 * * * *` | Every 15 minutes |

---

## 🔁 Retry & Failure Flow

```
Job fails → retryCount < maxRetries?
    YES → exponential backoff retry (1s × 2^n, max 60s)
    NO  → PERMANENTLY FAILED
              → email notification (or webhook) sent to user
```

---

## 📁 Project Structure

```
Chronos-job-scheduler/
├── Dockerfile                    # Backend Docker image
├── docker-compose.yml            # Full stack orchestration
├── .env.example                  # Environment template
├── src/main/java/com/chronos/
│   ├── config/                   # Security, Quartz, Worker
│   ├── controller/               # REST endpoints
│   ├── entity/                   # Job, User, ExecutionLog
│   ├── scheduler/                # Quartz + Redis queue
│   └── service/                  # Business logic + email
└── chronos-frontend/
    ├── Dockerfile                # Frontend Docker image
    ├── nginx.conf                # Reverse proxy config
    └── src/
        ├── api/                  # Axios + API calls
        ├── context/              # JWT auth state
        ├── components/           # UI components
        └── pages/                # All pages
```

---

## 🤝 Contributing

```bash
git checkout -b feature/your-feature
git commit -m "feat: your change"
git push origin feature/your-feature
# Then open a Pull Request on GitHub
```

---

## 📄 License

MIT — free to use for learning or production.

---

<div align="center">
Made with ☕ and way too many stack traces &nbsp;|&nbsp; <a href="https://github.com/0shubham2802">@0shubham2802</a>
</div>
