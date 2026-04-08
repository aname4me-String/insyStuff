-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- ── Document metadata ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.document_metadata (
  id                BIGSERIAL PRIMARY KEY,
  file_name         TEXT,
  source_path       TEXT,
  pdf_title         TEXT,
  pdf_author        TEXT,
  pdf_subject       TEXT,
  pdf_keywords      TEXT,
  creation_ts       TIMESTAMPTZ,
  modification_ts   TIMESTAMPTZ,
  total_pages       INT,
  vector_store_type TEXT
);

-- ── Vector store (Spring AI default schema) ──────────────────────────────────
CREATE TABLE IF NOT EXISTS public.vector_store (
  id        TEXT PRIMARY KEY,
  content   TEXT,
  metadata  JSONB,
  embedding vector(768) NOT NULL
);

-- ── Chunk ↔ document mapping ─────────────────────────────────────────────────
-- Note: vector_id intentionally has NO FK to vector_store because chunks may
-- belong to either the pgvector store (rows in vector_store) or the in-memory
-- SimpleVectorStore (no DB row).  The FK was removed to support both backends.
CREATE TABLE IF NOT EXISTS public.vector_store_document_chunk (
  vector_id    TEXT   NOT NULL,
  document_id  BIGINT NOT NULL REFERENCES public.document_metadata(id)    ON DELETE CASCADE,
  chunk_index  INT,
  page_number  INT,
  total_chunks INT,
  PRIMARY KEY (vector_id, document_id)
);

-- ── Indexes ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
  ON public.vector_store USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE INDEX IF NOT EXISTS idx_vector_store_doc_chunk_document_id
  ON public.vector_store_document_chunk (document_id);

CREATE INDEX IF NOT EXISTS idx_vector_store_doc_chunk_vector_id
  ON public.vector_store_document_chunk (vector_id);

CREATE INDEX IF NOT EXISTS idx_document_metadata_file_name
  ON public.document_metadata (file_name);
