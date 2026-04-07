package at.insystuff.rag.core.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "vector_store_document_chunk", schema = "public")
@IdClass(VectorStoreDocumentChunkId.class)
@Data
public class VectorStoreDocumentChunk {

    @Id
    private String vectorId;

    @Id
    private Long documentId;

    private Integer chunkIndex;
    private Integer pageNumber;
    private Integer totalChunks;
}
