package at.insystuff.rag.core.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Entity
@Table(name = "document_metadata", schema = "public")
@Data
public class DocumentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String sourcePath;
    private String pdfTitle;
    private String pdfAuthor;
    private String pdfSubject;
    private String pdfKeywords;

    @Column(name = "creation_ts")
    private OffsetDateTime creationTs;

    @Column(name = "modification_ts")
    private OffsetDateTime modificationTs;

    private Integer totalPages;

    private String vectorStoreType;
}
