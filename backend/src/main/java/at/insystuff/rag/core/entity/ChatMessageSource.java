package at.insystuff.rag.core.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "chat_message_source", schema = "public")
@Data
public class ChatMessageSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "page_number")
    private Integer pageNumber;
}
