package at.insystuff.rag.core.repository;

import at.insystuff.rag.core.entity.VectorStoreDocumentChunk;
import at.insystuff.rag.core.entity.VectorStoreDocumentChunkId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VectorStoreDocumentChunkRepository
        extends JpaRepository<VectorStoreDocumentChunk, VectorStoreDocumentChunkId> {

    List<VectorStoreDocumentChunk> findByVectorIdIn(List<String> vectorIds);

    List<VectorStoreDocumentChunk> findByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);
}
