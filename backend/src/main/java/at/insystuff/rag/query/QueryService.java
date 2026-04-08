package at.insystuff.rag.query;

import at.insystuff.rag.core.entity.DocumentMetadata;
import at.insystuff.rag.core.entity.VectorStoreDocumentChunk;
import at.insystuff.rag.core.repository.DocumentMetadataRepository;
import at.insystuff.rag.core.repository.VectorStoreDocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryService {

    private static final String PROMPT_TEMPLATE = """
            You are a helpful assistant. Answer the question using ONLY the context provided below.
            If the context does not contain enough information, say so honestly.
            Do not make up information that is not present in the context.

            Context:
            {context}

            Question: {question}

            Answer:
            """;

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final VectorStoreDocumentChunkRepository chunkRepository;
    private final DocumentMetadataRepository documentMetadataRepository;

    public ChatResponse answer(String question, String model) {
        StreamContext ctx = buildStreamContext(question, model);
        if (ctx.tokenStream() == null) {
            return new ChatResponse(NO_DOCS_MESSAGE, List.of());
        }
        String answer = ctx.tokenStream()
                .collect(Collectors.joining())
                .block();
        return new ChatResponse(answer != null ? answer : "", ctx.sources());
    }

    public StreamContext streamAnswer(String question, String model) {
        return buildStreamContext(question, model);
    }

    private static final String NO_DOCS_MESSAGE =
            "I could not find any relevant documents to answer your question.";

    private StreamContext buildStreamContext(String question, String model) {
        // 1. Retrieve top-k relevant chunks
        List<Document> relevant = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(5).build()
        );

        if (relevant.isEmpty()) {
            Flux<String> single = Flux.just(NO_DOCS_MESSAGE);
            return new StreamContext(List.of(), single);
        }

        // 2. Build context string
        String context = relevant.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 3. Resolve sources before streaming
        List<String> vectorIds = relevant.stream().map(Document::getId).toList();
        List<VectorStoreDocumentChunk> chunks = chunkRepository.findByVectorIdIn(vectorIds);
        List<SourceReference> sources = chunks.stream()
                .map(chunk -> {
                    Optional<DocumentMetadata> meta = documentMetadataRepository.findById(chunk.getDocumentId());
                    String fileName = meta.map(DocumentMetadata::getFileName).orElse("unknown");
                    return new SourceReference(fileName, chunk.getPageNumber());
                })
                .distinct()
                .toList();

        // 4. Build prompt
        PromptTemplate promptTemplate = new PromptTemplate(PROMPT_TEMPLATE);
        Prompt prompt;
        if (model != null && !model.isBlank()) {
            OllamaChatOptions options = OllamaChatOptions.builder().model(model).build();
            prompt = promptTemplate.create(Map.of("context", context, "question", question), options);
        } else {
            prompt = promptTemplate.create(Map.of("context", context, "question", question));
        }

        // 5. Stream tokens — null-safe chain through getResult → getOutput → getText
        Flux<String> tokenStream = chatModel.stream(prompt)
                .mapNotNull(r -> r.getResult())
                .mapNotNull(result -> result.getOutput())
                .mapNotNull(output -> output.getText())
                .filter(t -> !t.isEmpty());

        log.info("Prepared stream for question with {} source chunks, model='{}'",
                chunks.size(), model != null && !model.isBlank() ? model : "default");
        return new StreamContext(sources, tokenStream);
    }

    public record SourceReference(String fileName, Integer pageNumber) {}

    public record ChatResponse(String answer, List<SourceReference> sources) {}

    public record StreamContext(List<SourceReference> sources, Flux<String> tokenStream) {}
}
