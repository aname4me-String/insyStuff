# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [1.0.0-SNAPSHOT] - 2025-07-01

### Added
- Initial project creation
- Spring Boot 3.3.5 backend with Spring AI 1.0.0
- PDF indexing via `PagePdfDocumentReader` and `TokenTextSplitter`
- Vector similarity search powered by pgvector (PostgreSQL 16)
- Ollama integration for local LLM chat (`llama3.2`) and embeddings (`nomic-embed-text`)
- `DocumentMetadata` JPA entity and repository to track indexed PDFs
- `VectorStoreDocumentChunk` JPA entity linking vector store entries to document metadata
- RAG query pipeline with source attribution (file name + page number)
- CORS enabled for all origins (development-friendly)
- Vanilla JS / HTML / CSS single-page frontend
  - Chat interface with user/assistant bubbles
  - Source badges per assistant message
  - PDF upload panel
  - Indexed documents list
- nginx reverse proxy (`/api/` → Spring Boot backend)
- Docker Compose stack: postgres (pgvector), adminer, ollama, backend, frontend
- Multi-stage Dockerfile for backend (Maven build → JRE runtime)
- PostgreSQL init script creating `vector_store`, `document_metadata`, and
  `vector_store_document_chunk` tables with indexes
- `.env.example` with all configurable environment variables

## [1.1.0] – 2026-04-08

### Changed
- Backend build system: Maven → Gradle 9.2.1 (matches project Dockerfile)
- Frontend: vanilla HTML/JS → Angular 19 SPA (Chat + Documents views)
- Docker: renamed Dockerfiles to `db.DOCKERFILE`, `backend.DOCKERFILE`, `frontend.DOCKERFILE`
- Docker Compose: adopted project network style (`tmoate_DB` / `tmoate_APP`), healthcheck-based `depends_on`, Adminer on `adminer` profile
- Environment variables aligned: `user` / `pass` / `signin` / `POSTGRES_URL` / `PORT`
- Added `ollama` service to compose; `nginx.conf` moved into `frontend/` (copied by `frontend.DOCKERFILE`)
- DB init script creates pgvector + full schema in `venlab` database
- Spring Boot Actuator readiness probe (`/actuator/health/readiness`) for Docker healthcheck
