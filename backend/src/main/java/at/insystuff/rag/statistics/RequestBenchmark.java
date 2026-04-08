package at.insystuff.rag.statistics;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Entity
@Table(name = "request_benchmark", schema = "public")
@Data
public class RequestBenchmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private OffsetDateTime timestamp;

    @Column(length = 300)
    private String question;

    private String model;

    private String vectorStoreType;

    /** Duration of the vector similarity search in milliseconds. */
    private Long vectorSearchMs;

    /** Total wall-clock time from request start to last token in milliseconds. */
    private Long totalResponseMs;

    /** Number of tokens streamed back to the client. */
    private Integer tokenCount;

    /** Number of source chunks retrieved from the vector store. */
    private Integer sourceCount;

    /** JVM heap used (in MB) at the time the benchmark was recorded. */
    private Long ramUsedMb;

    /** System CPU load percentage (0–100) at the time of recording. */
    private Double cpuLoadPercent;
}
