package at.insystuff.rag.indexer;

import at.insystuff.rag.core.entity.DocumentMetadata;
import at.insystuff.rag.core.entity.VectorStoreDocumentChunk;
import at.insystuff.rag.core.repository.DocumentMetadataRepository;
import at.insystuff.rag.core.repository.VectorStoreDocumentChunkRepository;
import at.insystuff.rag.vectorstore.VectorStoreRouter;
import at.insystuff.rag.vectorstore.VectorStoreType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexerService {

    private final VectorStoreRouter vectorStoreRouter;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final VectorStoreDocumentChunkRepository chunkRepository;

    // Keep chunks small enough for the embedding model's context window (num-ctx: 8192).
    // TokenTextSplitter(chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator)
    private static final int CHUNK_SIZE_TOKENS = 512;
    // Embed one chunk per vectorStore.add() call to avoid exceeding the embedding model's num-ctx.
    // Ollama's /api/embed counts total tokens across all inputs in a single request; sending one
    // chunk at a time guarantees that each request stays within the 8192-token context window
    // regardless of tokenizer differences between TokenTextSplitter (cl100k_base) and nomic-embed-text.
    private static final int EMBEDDING_BATCH_SIZE = 1;

    @Transactional
    public void indexPdf(MultipartFile multipartFile, VectorStoreType vectorStoreType) {
        try {
            byte[] bytes = multipartFile.getBytes();
            String fileName = multipartFile.getOriginalFilename();
            Resource resource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };

            PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                    .withPagesPerDocument(1)
                    .build();

            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource, config);
            List<Document> rawPages = pdfReader.get();

            TokenTextSplitter splitter = new TokenTextSplitter(CHUNK_SIZE_TOKENS, 100, 5, 10000, true);
            List<Document> chunks = splitter.apply(rawPages);

            // Persist document metadata
            DocumentMetadata metadata = new DocumentMetadata();
            metadata.setFileName(fileName);
            metadata.setSourcePath(fileName);
            metadata.setCreationTs(OffsetDateTime.now());
            metadata.setModificationTs(OffsetDateTime.now());
            metadata.setTotalPages(rawPages.size());
            metadata.setVectorStoreType(vectorStoreType.name());

            if (!rawPages.isEmpty()) {
                java.util.Map<String, Object> firstMeta = rawPages.get(0).getMetadata();
                metadata.setPdfTitle(stringOrNull(firstMeta.get("pdf_title")));
                metadata.setPdfAuthor(stringOrNull(firstMeta.get("pdf_author")));
                metadata.setPdfSubject(stringOrNull(firstMeta.get("pdf_subject")));
                metadata.setPdfKeywords(stringOrNull(firstMeta.get("pdf_keywords")));
            }

            DocumentMetadata saved = documentMetadataRepository.save(metadata);
            log.info("Saved document metadata id={} fileName={} vectorStoreType={}", saved.getId(), fileName, vectorStoreType);

            // Add chunks to vector store, sanitizing null bytes that PostgreSQL rejects
            List<Document> sanitizedChunks = chunks.stream()
                    .map(chunk -> {
                        String clean = chunk.getText().replace("\u0000", "");
                        return new Document(chunk.getId(), clean, chunk.getMetadata());
                    })
                    .toList();

            // Ensure the router targets the requested store before adding vectors
            vectorStoreRouter.setActiveType(vectorStoreType);

            // Embed in small batches to avoid exceeding the embedding model's context window
            for (int i = 0; i < sanitizedChunks.size(); i += EMBEDDING_BATCH_SIZE) {
                List<Document> batch = sanitizedChunks.subList(i, Math.min(i + EMBEDDING_BATCH_SIZE, sanitizedChunks.size()));
                vectorStoreRouter.add(batch);
            }

            // Persist chunk-to-document mapping
            int totalChunks = sanitizedChunks.size();
            for (int i = 0; i < sanitizedChunks.size(); i++) {
                Document chunk = sanitizedChunks.get(i);
                String vectorId = chunk.getId();
                Object pageObj = chunk.getMetadata().get("page_number");
                Integer pageNumber = pageObj instanceof Number n ? n.intValue() : null;

                VectorStoreDocumentChunk chunkEntity = new VectorStoreDocumentChunk();
                chunkEntity.setVectorId(vectorId);
                chunkEntity.setDocumentId(saved.getId());
                chunkEntity.setChunkIndex(i);
                chunkEntity.setPageNumber(pageNumber);
                chunkEntity.setTotalChunks(totalChunks);
                chunkRepository.save(chunkEntity);
            }

            log.info("Indexed {} chunks for document '{}' in {}", totalChunks, fileName, vectorStoreType);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded PDF", e);
        }
    }

    @Transactional
    public boolean deleteDocument(Long id) {
        Optional<DocumentMetadata> opt = documentMetadataRepository.findById(id);
        if (opt.isEmpty()) {
            return false;
        }
        DocumentMetadata doc = opt.get();
        // Switch the router to the store where this document's vectors actually live
        if (doc.getVectorStoreType() != null) {
            try {
                vectorStoreRouter.setActiveType(VectorStoreType.valueOf(doc.getVectorStoreType()));
            } catch (IllegalArgumentException ignored) {
                // Unknown type — fall back to whatever is currently active
            }
        }
        List<VectorStoreDocumentChunk> chunks = chunkRepository.findByDocumentId(id);
        if (!chunks.isEmpty()) {
            List<String> vectorIds = chunks.stream().map(VectorStoreDocumentChunk::getVectorId).toList();
            vectorStoreRouter.delete(vectorIds);
            chunkRepository.deleteByDocumentId(id);
        }
        documentMetadataRepository.deleteById(id);
        log.info("Deleted document id={} fileName={}", id, doc.getFileName());
        return true;
    }

    @Transactional
    public Optional<DocumentMetadata> renameDocument(Long id, String newName) {
        return documentMetadataRepository.findById(id).map(doc -> {
            doc.setFileName(newName);
            doc.setModificationTs(OffsetDateTime.now());
            DocumentMetadata saved = documentMetadataRepository.save(doc);
            log.info("Renamed document id={} to '{}'", id, newName);
            return saved;
        });
    }

    public List<DocumentMetadata> listDocuments(String vectorStoreType) {
        if (vectorStoreType != null && !vectorStoreType.isBlank()) {
            return documentMetadataRepository.findByVectorStoreType(vectorStoreType.toUpperCase());
        }
        return documentMetadataRepository.findAll();
    }

    private String stringOrNull(Object value) {
        return value != null ? value.toString() : null;
    }
}
