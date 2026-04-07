# insyStuff – Local RAG (Retrieval-Augmented Generation)

A fully local RAG system built with Spring Boot, Spring AI, pgvector, Ollama, and a Vanilla JS frontend.  
All data and models stay on your machine – no cloud services required.

---

## Prerequisites

| Tool | Version |
|---|---|
| Docker & Docker Compose | ≥ 2.20 |
| Ollama | ≥ 0.3 (only if running outside Docker) |
| Java 21 + Maven 3.9 | only for local dev without Docker |

---

## Quick Start

```bash
# 1. Clone the repo
git clone <repo-url>
cd insyStuff

# 2. Copy env file and adjust if needed
cp docker/.env.example docker/.env

# 3. Start all services
cd docker
docker compose up -d --build

# 4. Pull required Ollama models (first time only)
docker exec -it docker-ollama-1 ollama pull llama3.2
docker exec -it docker-ollama-1 ollama pull nomic-embed-text

# 5. Open the UI
open http://localhost
```

---

## Architecture

```
┌─────────────┐     HTTP      ┌──────────────┐    JDBC     ┌──────────────────┐
│  Browser    │ ────────────▶ │   nginx      │             │   PostgreSQL 16  │
│  (port 80)  │               │  (frontend)  │             │   + pgvector     │
└─────────────┘               └──────┬───────┘             └────────┬─────────┘
                                     │ /api/*                        │
                                     ▼                               │
                              ┌──────────────┐  Spring Data JPA ────┘
                              │  Spring Boot │
                              │  backend     │ ──── Spring AI ────▶ Ollama
                              │  (port 8080) │                    (embeddings + chat)
                              └──────────────┘
```

| Component | Technology |
|---|---|
| Backend | Spring Boot 3.3.5, Spring AI 1.0.0, Java 21 |
| Vector DB | PostgreSQL 16 + pgvector |
| LLM / Embeddings | Ollama (llama3.2 / nomic-embed-text) |
| Frontend | Vanilla HTML/CSS/JS served by nginx |
| DB Admin | Adminer on port 8090 |

---

## Environment Variables

All variables live in `docker/.env` (copy from `docker/.env.example`):

| Variable | Default | Description |
|---|---|---|
| `DB_USER` | `raguser` | PostgreSQL username |
| `DB_PASSWORD` | `ragpassword` | PostgreSQL password |
| `DB_NAME` | `ragdb` | PostgreSQL database name |
| `OLLAMA_BASE_URL` | `http://ollama:11434` | Ollama API endpoint |
| `CHAT_MODEL` | `llama3.2` | Ollama chat model |
| `EMBEDDING_MODEL` | `nomic-embed-text` | Ollama embedding model |
| `BACKEND_HOST` | `backend` | Backend hostname (nginx proxy) |
| `BACKEND_PORT` | `8080` | Backend port (nginx proxy) |

---

## How to Index PDFs

### Via the UI
1. Open **http://localhost**
2. Click **Upload PDF** and select a file
3. Wait for the success notification

### Via curl
```bash
curl -F "file=@/path/to/document.pdf" http://localhost/api/index
```

### List indexed documents
```bash
curl http://localhost/api/documents
```

---

## How to Use the Chat

1. Open **http://localhost**
2. Type a question in the input field and press **Enter** or click **Send**
3. The assistant answers using only the indexed documents and lists the sources below each reply

### Via curl
```bash
curl -X POST http://localhost/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "What does the document say about X?"}'
```

---

## Local Development (without Docker)

```bash
# Start only infrastructure
cd docker
docker compose up -d postgres ollama adminer

# Run backend
cd backend
mvn spring-boot:run

# Serve frontend (any static server)
cd frontend
npx serve .
```

---

## Services & Ports

| Service | URL |
|---|---|
| Frontend (UI) | http://localhost |
| Backend API | http://localhost:8080 |
| Adminer (DB UI) | http://localhost:8090 |
| Ollama API | http://localhost:11434 |
| PostgreSQL | localhost:5432 |
