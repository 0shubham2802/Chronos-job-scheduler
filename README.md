# ⏱️ Chronos — Job Scheduler

<div align="center">

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React_18-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_17-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Quartz](https://img.shields.io/badge/Quartz_Scheduler-4A90D9?style=for-the-badge&logoColor=white)

**A production-grade job scheduling platform with a modern React frontend, distributed execution, and email notifications.**

[Features](#-features) · [Tech Stack](#-tech-stack) · [Getting Started](#-getting-started) · [API Docs](#-api-reference) · [Screenshots](#-screenshots)

</div>

---

## ✨ Features

| Feature | Description |
|---|---|
| 🕐 **One-time Jobs** | Schedule a job to run once at a specific date and time |
| 🔁 **Recurring Jobs** | Use cron expressions for repeating schedules |
| ▶️ **Manual Trigger** | Run any job immediately on demand |
| ⏸️ **Pause / Resume** | Pause recurring jobs and resume them later |
| 🔄 **Retry with Backoff** | Auto-retry failed jobs with exponential backoff (1s → 2s → 4s → ...) |
| 📧 **Email Notifications** | Send scheduled emails or get notified when a job permanently fails |
| 📊 **Execution Logs** | Full history of every run — status, duration, error messages |
| 🔐 **JWT Auth** | Secure login and registration with JWT tokens |
| 🚦 **Distributed Locking** | Redis-based locks prevent duplicate job execution |
| 📡 **Webhook Support** | POST to an external URL when a job fails |

---

## 🛠️ Tech Stack

### Backend
- **Spring Boot 3.3** — REST API framework
- **Quartz Scheduler** — Job scheduling engine (persisted in PostgreSQL)
- **Spring Data JPA + Hibernate** — Database ORM
- **PostgreSQL** — Primary database (jobs, users, execution logs)
- **Redis** — Job queue + distributed locking
- **Spring Security + JWT** — Authentication
- **JavaMailSender** — Email notifications via SMTP
- **Flyway** — Database migrations
- **Micrometer + Prometheus** — Metrics and monitoring

### Frontend
- **React 18 + Vite** — Fast frontend build
- **React Router v6** — Client-side routing
- **Axios** — HTTP client with JWT interceptors
- **React Hook Form** — Form validation
- **date-fns** — Date formatting
- **Custom CSS Design System** — No Tailwind dependency

---

## 🏗️ Architecture

```
Browser (React)
      │
      │  JWT Auth + REST API
      ▼
Spring Boot (port 8080)
      │
      ├── JobService ──────────► PostgreSQL (jobs, users, logs)
      │
      ├── SchedulerService ────► Quartz (triggers, cron)
      │                               │
      │                               │ fires at scheduled time
      │                               ▼
      └── JobQueueService ◄──── ChronosJobExecutor
                │
                ├── Redis Queue (distributed FIFO)
                ├── Redis Lock (prevent duplicate runs)
                └── JobExecutionService
                          │
                          ├── Send email (if payload has "to")
                          ├── Simulate job (default)
                          └── RetryService (on failure)
                                    │
                                    └── NotificationService (email/webhook)
```

---

## 🚀 Getting Started

### Prerequisites

Make sure you have these installed:

```bash
java --version      # Java 17+
psql --version      # PostgreSQL 14+
redis-cli ping      # Redis (should print PONG)
node --version      # Node.js 18+
```

### 1. Clone the repository

```bash
git clone https://github.com/0shubham2802/Chronos-job-scheduler.git
cd Chronos-job-scheduler
```

### 2. Set up the database

```bash
# Create the database
psql -U postgres -c "CREATE DATABASE chronos;"
```

### 3. Configure the backend

Edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/chronos
spring.datasource.username=postgres
spring.datasource.password=your_password

# Email (Gmail)
spring.mail.username=your@gmail.com
spring.mail.password=your_16_char_app_password   # Google App Password
chronos.notification.enabled=true
```

> **Gmail App Password:** Go to [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords), create an app password named "Chronos", and paste the 16-character code above.

### 4. Start Redis

```bash
brew services start redis   # macOS
# or
redis-server                # Linux
```

### 5. Run the backend

```bash
./gradlew bootRun -x test
```

Backend starts on **http://localhost:8080**

### 6. Run the frontend

```bash
cd chronos-frontend
npm install
npm run dev
```

Frontend starts on **http://localhost:5173**

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
    "body": "Hi, here is your weekly report."
  }
}
```

### Test Retry Behaviour

```json
{
  "name": "Failing Job Test",
  "type": "ONE_TIME",
  "scheduledAt": "2026-05-10T10:00:00",
  "timezone": "UTC",
  "maxRetries": 3,
  "payload": {
    "fail": true
  }
}
```

### Webhook on Failure

```json
{
  "payload": {
    "webhookUrl": "https://your-server.com/webhook"
  }
}
```

---

## 📡 API Reference

### Auth

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Create a new account |
| `POST` | `/api/auth/login` | Login and receive JWT |
| `POST` | `/api/auth/refresh` | Refresh an expired token |

### Jobs

All job endpoints require `Authorization: Bearer <token>` header.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/jobs` | List all jobs (paginated) |
| `POST` | `/api/jobs` | Create a new job |
| `GET` | `/api/jobs/:id` | Get job details |
| `PATCH` | `/api/jobs/:id` | Update a job |
| `DELETE` | `/api/jobs/:id` | Delete a job |
| `POST` | `/api/jobs/:id/trigger` | Run job immediately |
| `POST` | `/api/jobs/:id/pause` | Pause a recurring job |
| `POST` | `/api/jobs/:id/resume` | Resume a paused job |
| `GET` | `/api/jobs/:id/logs` | Get execution history |

---

## ⚙️ Cron Expression Guide

```
┌───── minute (0-59)
│ ┌───── hour (0-23)
│ │ ┌───── day of month (1-31)
│ │ │ ┌───── month (1-12)
│ │ │ │ ┌───── day of week (SUN-SAT)
│ │ │ │ │
* * * * *
```

| Expression | Meaning |
|------------|---------|
| `* * * * *` | Every minute |
| `0 * * * *` | Every hour |
| `0 9 * * *` | Every day at 9am |
| `0 9 * * MON` | Every Monday at 9am |
| `0 0 1 * *` | First of every month at midnight |
| `0 9,17 * * MON-FRI` | 9am and 5pm on weekdays |

---

## 🔁 Retry & Failure Handling

When a job fails, Chronos automatically retries with exponential backoff:

```
Attempt 1 fails → wait 1s  → retry
Attempt 2 fails → wait 2s  → retry
Attempt 3 fails → wait 4s  → retry
Attempt 4 fails → PERMANENTLY FAILED → email notification sent
```

Configure in `application.properties`:
```properties
chronos.retry.initial-delay-ms=1000
chronos.retry.multiplier=2.0
chronos.retry.max-delay-ms=60000
```

---

## 📁 Project Structure

```
chronos/                          # Backend (Spring Boot)
├── src/main/java/com/chronos/
│   ├── config/                   # Security, Quartz, Worker configs
│   ├── controller/               # REST controllers
│   ├── dto/                      # Request/Response objects
│   ├── entity/                   # JPA entities (Job, User, ExecutionLog)
│   ├── repository/               # Spring Data repositories
│   ├── scheduler/                # Quartz executor, job queue
│   ├── security/                 # JWT filter, auth service
│   └── service/                  # Business logic
├── src/main/resources/
│   ├── application.properties    # Configuration
│   └── db/migration/             # Flyway SQL migrations
└── build.gradle

chronos-frontend/                 # Frontend (React + Vite)
├── src/
│   ├── api/                      # Axios + API functions
│   ├── context/                  # AuthContext
│   ├── components/               # Navbar, StatusBadge, ProtectedRoute
│   └── pages/                    # Login, Register, Dashboard, Create, Detail
└── package.json
```

---

## 🌍 Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `spring.datasource.url` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/chronos` |
| `spring.mail.username` | Gmail address for sending emails | — |
| `spring.mail.password` | Gmail App Password | — |
| `chronos.notification.enabled` | Enable email notifications | `false` |
| `jwt.secret` | Secret key for JWT signing | — |
| `spring.data.redis.host` | Redis host | `localhost` |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "feat: add your feature"`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request on GitHub

---

## 📄 License

MIT License — feel free to use this project for learning or production.

---

<div align="center">
  Made with ☕ and way too many stack traces &nbsp;|&nbsp; <a href="https://github.com/0shubham2802">@0shubham2802</a>
</div>
