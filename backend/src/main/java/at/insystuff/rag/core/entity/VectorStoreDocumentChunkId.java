package at.insystuff.rag.core.entity;

import java.io.Serializable;
import java.util.Objects;

public class VectorStoreDocumentChunkId implements Serializable {

    private String vectorId;
    private Long documentId;

    public VectorStoreDocumentChunkId() {}

    public VectorStoreDocumentChunkId(String vectorId, Long documentId) {
        this.vectorId = vectorId;
        this.documentId = documentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VectorStoreDocumentChunkId other)) return false;
        return Objects.equals(vectorId, other.vectorId) && Objects.equals(documentId, other.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vectorId, documentId);
    }
}
