# insyStuff – Local RAG (Retrieval-Augmented Generation)

A fully local RAG system: upload private PDFs, ask questions in chat, and receive answers with page-level source references — no cloud services required.

| Component | Technology |
|---|---|
| Backend | Spring Boot 3.5.11 · Spring AI 1.1.1 · Gradle 9.2.1 · Java 17 |
| Vector DB | PostgreSQL 13 (de_AT locale) + pgvector |
| LLM / Embeddings | Ollama (`llama3.2` / `nomic-embed-text`) |
| Frontend | Angular 19 served by nginx |
| DB Admin | Adminer 5 (optional profile) |

---

## Quick Start

```bash
# 1. Clone the repo
git clone <repo-url> && cd insyStuff

# 2. Configure credentials
cp docker/.env.example docker/.env
# Edit docker/.env and set: user, pass, signin

# 3. Build & start all services
cd docker
docker compose up -d --build

# 4. Pull Ollama models (first time only – can take a few minutes)
docker compose exec ollama ollama pull llama3.2
docker compose exec ollama ollama pull nomic-embed-text

# 5. Open the app
open http://localhost
```

---

## Architecture

```
Browser (port 80)
  └── nginx (frontend)
        ├── /           → Angular SPA
        └── /api/*      → http://backend:8080/api/*
                              ├── POST /api/index    (upload PDF)
                              ├── GET  /api/documents (list indexed docs)
                              └── POST /api/chat      (RAG query)
                                        ├── pgvector similarity search
                                        └── Ollama chat (llama3.2)
```

Networks:
- **tmoate_DB** — db ↔ backend (PostgreSQL)
- **tmoate_APP** — backend ↔ frontend ↔ ollama

---

## Environment Variables (`docker/.env`)

| Variable | Default | Description |
|---|---|---|
| `user` | — | PostgreSQL username |
| `pass` | — | PostgreSQL password |
| `signin` | — | JWT / session signing secret |
| `APP_PORT` | `80` | Host port for the frontend |
| `ADMINER_PORT` | `8081` | Host port for Adminer (profile only) |
| `CHAT_MODEL` | `llama3.2` | Ollama chat model |
| `EMBEDDING_MODEL` | `nomic-embed-text` | Ollama embedding model |

---

## Services & Ports

| Service | URL | Notes |
|---|---|---|
| Frontend (UI) | http://localhost | Angular SPA + API proxy |
| Backend API | internal (8080) | Only reachable via nginx |
| Ollama | internal (11434) | Only reachable inside tmoate_APP |
| PostgreSQL | internal (5432) | Only reachable inside tmoate_DB |
| Adminer | http://localhost:8081 | `docker compose --profile adminer up` |

---

## Local Development (without Docker)

```bash
# Start infrastructure only
cd docker
docker compose up -d db ollama

# Backend
cd backend
./gradlew bootRun

# Frontend
cd frontend
npm install
npx ng serve        # http://localhost:4200  (proxies /api/ to localhost:8080)
```

---

## How to Index PDFs

### Via the UI
1. Open `http://localhost` → click **Documents** tab → **Upload PDF**

### Via curl
```bash
curl -F "file=@/path/to/document.pdf" http://localhost/api/index
```

---

## How to Chat

1. Open `http://localhost` → **Chat** tab
2. Type a question → press **Enter** or **➤**
3. The assistant answers using only your indexed documents and cites the source file + page number.

### Via curl
```bash
curl -X POST http://localhost/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"What does the document say about X?"}'
```

---

## CHANGELOG

See [CHANGELOG.md](CHANGELOG.md).
