package at.insystuff.rag.vectorstore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Routes all VectorStore operations to whichever backend is currently active.
 * Callers inject this bean (it is {@code @Primary}) and remain unaware of the
 * underlying store. The active store can be switched at runtime via
 * {@link #setActiveType(VectorStoreType)}.
 */
@Component
@Primary
@Slf4j
public class VectorStoreRouter implements VectorStore {

    private final PgVectorStore pgVectorStore;
    private final SimpleVectorStore simpleVectorStore;

    private final AtomicReference<VectorStoreType> activeType =
            new AtomicReference<>(VectorStoreType.PGVECTOR);

    public VectorStoreRouter(
            @Qualifier("pgVectorStore") PgVectorStore pgVectorStore,
            @Qualifier("simpleVectorStore") SimpleVectorStore simpleVectorStore) {
        this.pgVectorStore = pgVectorStore;
        this.simpleVectorStore = simpleVectorStore;
    }

    public VectorStoreType getActiveType() {
        return activeType.get();
    }

    public void setActiveType(VectorStoreType type) {
        activeType.set(type);
        log.info("Switched active vector store to {}", type);
    }

    private VectorStore active() {
        return activeType.get() == VectorStoreType.PGVECTOR ? pgVectorStore : simpleVectorStore;
    }

    @Override
    public void add(List<Document> documents) {
        active().add(documents);
    }

    @Override
    public void delete(List<String> idList) {
        active().delete(idList);
    }

    @Override
    public void delete(Filter.Expression expression) {
        active().delete(expression);
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        return active().similaritySearch(request);
    }
}
