package at.insystuff.rag.conversation;

import at.insystuff.rag.core.entity.Chat;
import at.insystuff.rag.core.entity.ChatMessage;
import at.insystuff.rag.core.entity.ChatMessageSource;
import at.insystuff.rag.core.repository.ChatMessageRepository;
import at.insystuff.rag.core.repository.ChatMessageSourceRepository;
import at.insystuff.rag.core.repository.ChatRepository;
import at.insystuff.rag.query.QueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private static final String DEFAULT_CHAT_TITLE = "New Chat";
    private static final String DEFAULT_MODEL_LABEL = "default";

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageSourceRepository chatMessageSourceRepository;
    private final QueryService queryService;

    public List<Chat> listChats() {
        return chatRepository.findAllByOrderByCreatedAtDesc();
    }

    public Chat createChat(String title) {
        Chat chat = new Chat();
        chat.setTitle(title != null && !title.isBlank() ? title : DEFAULT_CHAT_TITLE);
        chat.setCreatedAt(OffsetDateTime.now());
        return chatRepository.save(chat);
    }

    public Optional<Chat> findChat(Long chatId) {
        return chatRepository.findById(chatId);
    }

    @Transactional
    public void deleteChat(Long chatId) {
        List<ChatMessage> messages = chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        List<Long> messageIds = messages.stream().map(ChatMessage::getId).toList();
        if (!messageIds.isEmpty()) {
            chatMessageSourceRepository.deleteByMessageIdIn(messageIds);
        }
        chatMessageRepository.deleteByChatId(chatId);
        chatRepository.deleteById(chatId);
    }

    public List<ChatMessageDto> getMessages(Long chatId) {
        List<ChatMessage> messages = chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        return messages.stream().map(msg -> {
            List<ChatMessageSourceDto> sources = chatMessageSourceRepository
                    .findByMessageId(msg.getId())
                    .stream()
                    .map(s -> new ChatMessageSourceDto(s.getFileName(), s.getPageNumber()))
                    .toList();
            return new ChatMessageDto(msg.getId(), msg.getRole(), msg.getContent(), msg.getModel(), sources, msg.getCreatedAt());
        }).toList();
    }

    @Transactional
    public ChatMessageDto sendMessage(Long chatId, String question, String model) {
        saveUserMessage(chatId, question);
        QueryService.ChatResponse response = queryService.answer(question, model);
        return saveAssistantMessage(chatId, response.answer(),
                model != null && !model.isBlank() ? model : DEFAULT_MODEL_LABEL,
                response.sources());
    }

    @Transactional
    public void saveUserMessage(Long chatId, String question) {
        ChatMessage userMessage = new ChatMessage();
        userMessage.setChatId(chatId);
        userMessage.setRole("user");
        userMessage.setContent(question);
        userMessage.setCreatedAt(OffsetDateTime.now());
        chatMessageRepository.save(userMessage);

        chatRepository.findById(chatId).ifPresent(chat -> {
            if (DEFAULT_CHAT_TITLE.equals(chat.getTitle())) {
                String newTitle = question.length() > 50 ? question.substring(0, 50) + "…" : question;
                chat.setTitle(newTitle);
                chatRepository.save(chat);
            }
        });
    }

    @Transactional
    public ChatMessageDto saveAssistantMessage(Long chatId, String answer, String model,
                                               List<QueryService.SourceReference> sources) {
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setChatId(chatId);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(answer);
        assistantMessage.setModel(model);
        assistantMessage.setCreatedAt(OffsetDateTime.now());
        ChatMessage savedAssistant = chatMessageRepository.save(assistantMessage);

        List<ChatMessageSourceDto> sourceDtos = sources.stream()
                .map(src -> {
                    ChatMessageSource source = new ChatMessageSource();
                    source.setMessageId(savedAssistant.getId());
                    source.setFileName(src.fileName());
                    source.setPageNumber(src.pageNumber());
                    chatMessageSourceRepository.save(source);
                    return new ChatMessageSourceDto(src.fileName(), src.pageNumber());
                })
                .toList();

        log.info("Persisted assistant message in chat {} with model '{}'", chatId, model);
        return new ChatMessageDto(
                savedAssistant.getId(),
                savedAssistant.getRole(),
                savedAssistant.getContent(),
                savedAssistant.getModel(),
                sourceDtos,
                savedAssistant.getCreatedAt()
        );
    }

    public record ChatMessageSourceDto(String fileName, Integer pageNumber) {}

    public record ChatMessageDto(
            Long id,
            String role,
            String content,
            String model,
            List<ChatMessageSourceDto> sources,
            OffsetDateTime createdAt
    ) {}
}
