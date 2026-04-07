package at.insystuff.rag.indexer;

import at.insystuff.rag.core.entity.DocumentMetadata;
import at.insystuff.rag.core.entity.VectorStoreDocumentChunk;
import at.insystuff.rag.core.repository.DocumentMetadataRepository;
import at.insystuff.rag.core.repository.VectorStoreDocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexerService {

    private final VectorStore vectorStore;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final VectorStoreDocumentChunkRepository chunkRepository;

    @Transactional
    public void indexPdf(MultipartFile multipartFile) {
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

            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> chunks = splitter.apply(rawPages);

            // Persist document metadata
            DocumentMetadata metadata = new DocumentMetadata();
            metadata.setFileName(fileName);
            metadata.setSourcePath(fileName);
            metadata.setCreationTs(OffsetDateTime.now());
            metadata.setModificationTs(OffsetDateTime.now());
            metadata.setTotalPages(rawPages.size());

            if (!rawPages.isEmpty()) {
                java.util.Map<String, Object> firstMeta = rawPages.getFirst().getMetadata();
                metadata.setPdfTitle(stringOrNull(firstMeta.get("pdf_title")));
                metadata.setPdfAuthor(stringOrNull(firstMeta.get("pdf_author")));
                metadata.setPdfSubject(stringOrNull(firstMeta.get("pdf_subject")));
                metadata.setPdfKeywords(stringOrNull(firstMeta.get("pdf_keywords")));
            }

            DocumentMetadata saved = documentMetadataRepository.save(metadata);
            log.info("Saved document metadata id={} fileName={}", saved.getId(), fileName);

            // Add chunks to vector store
            vectorStore.add(chunks);

            // Persist chunk-to-document mapping
            int totalChunks = chunks.size();
            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);
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

            log.info("Indexed {} chunks for document '{}'", totalChunks, fileName);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded PDF", e);
        }
    }

    public List<DocumentMetadata> listDocuments() {
        return documentMetadataRepository.findAll();
    }

    private String stringOrNull(Object value) {
        return value != null ? value.toString() : null;
    }
}
