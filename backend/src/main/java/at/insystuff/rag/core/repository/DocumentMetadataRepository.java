package at.insystuff.rag.core.repository;

import at.insystuff.rag.core.entity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, Long> {

    List<DocumentMetadata> findByVectorStoreType(String vectorStoreType);
}
