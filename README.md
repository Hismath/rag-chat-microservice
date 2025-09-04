# RAG Chat Storage Microservice

Introduction

This is a production-ready backend microservice designed to store and manage chat histories for a RAG (Retrieval-Augmented Generation) based chatbot system. It provides secure, scalable APIs to handle chat sessions, messages, and user management. The service is built following best practices for security, configuration, and error handling.

**RAG (Retrieval-Augmented Generation)** based chatbot system. 

Built with **Spring Boot**, **MySQL**, and **Swagger/OpenAPI**. Secured with **API Key authentication** and **rate limiting**.

---

## ✨ Features
- Create & manage **chat sessions** (rename, mark favorite, soft delete).
- Store **messages** with sender, content, and optional context.
- Automatically generate **AI responses** and refresh them on message update.
- **API Key authentication** (from `.env`).
- **Rate limiting** per API key (configurable).
- **Soft delete** support for sessions & messages.
- **Pagination** when retrieving chat messages.
- Centralized logging & error handling.
- **Swagger/OpenAPI documentation**.
- **Actuator** health/info endpoints.
- Dockerized with MySQL & Adminer.

---

## ⚙️ Tech Stack
- Java 17 / Spring Boot 3.x  
- Spring Data JPA + Hibernate  
- MySQL 8  
- Bucket4j + Caffeine (rate limiting & caching)  
- Swagger / springdoc-openapi  
- Docker & Docker Compose  

---

## 🚀 Getting Started

### 1. Clone the repo
```bash
git clone https://github.com/Hismath/rag-chat-microservice.git

cd rag-chat-microservice

2. Configure environment variables

Copy .env.example → .env and fill in your values:

cp .env.example .env


Example:

API_KEY=secret123
DB_URL=jdbc:mysql://localhost:3306/ragdb_chatSystem?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=raguser
DB_PASSWORD=ragpass
SERVER_PORT=8082
GEMINI_API_KEY=your-gemini-key
RATE_LIMIT_PERMITS=10
RATE_LIMIT_WINDOW_SECONDS=60
CORS_ALLOWED_ORIGINS=http://localhost:3000
SPRING_PROFILES_ACTIVE=dev

3. Run with Docker Compose
docker-compose up --build

This will start:

db: MySQL database

adminer: Adminer UI at http://localhost:8081

app: Spring Boot microservice on http://localhost:8082

📡 API Authentication

All APIs require this header:

X-API-KEY: <your-api-key>

The key is loaded from .env (API_KEY).

📖 API Documentation

Swagger UI:
👉 http://localhost:8082/swagger-ui.html

Authorize in Swagger using your X-API-KEY.

🛠️ Available Endpoints

Sessions

POST /api/sessions → Create a session

GET /api/sessions/{sessionId} → Get session by ID

PATCH /api/sessions/{sessionId} → Update (rename, mark favorite)

DELETE /api/sessions/{sessionId} → Soft delete session

GET /api/sessions/user/{userId} → Get all sessions for a user

GET /api/sessions/{userId}/favorites → Get favorite sessions for a user

Messages

POST /api/sessions/{sessionId}/messages → Add user message + AI response

GET /api/sessions/{sessionId}/messages → Get messages (paginated)

PATCH /api/sessions/{sessionId}/messages/{messageId} → Update user message (AI response regenerated)

DELETE /api/sessions/{sessionId}/messages/{messageId} → Delete message (+ linked AI reply if exists)

Health & Info

GET /actuator/health → Health check

GET /actuator/info → Service metadata

🔐 Security

API key required (X-API-KEY).

Rate limiting enforced per API key (default: 10 requests/minute).

CORS restricted via CORS_ALLOWED_ORIGINS.

🧪 Testing

Run unit tests:

mvn test

🐳 Database Admin

Adminer is available at:

URL: http://localhost:8081

System: MySQL

Server: db

Username/Password → from .env

📝 Roadmap

Add Flyway migrations

Improve test coverage

CI/CD pipeline integration