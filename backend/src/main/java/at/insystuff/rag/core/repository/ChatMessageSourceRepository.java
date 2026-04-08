package at.insystuff.rag.core.repository;

import at.insystuff.rag.core.entity.ChatMessageSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageSourceRepository extends JpaRepository<ChatMessageSource, Long> {
    List<ChatMessageSource> findByMessageId(Long messageId);
    void deleteByMessageIdIn(List<Long> messageIds);
}
