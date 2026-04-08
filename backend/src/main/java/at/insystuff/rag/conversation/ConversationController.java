package at.insystuff.rag.conversation;

import at.insystuff.rag.core.entity.Chat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public ResponseEntity<List<Chat>> listChats() {
        return ResponseEntity.ok(conversationService.listChats());
    }

    @PostMapping
    public ResponseEntity<Chat> createChat(@RequestBody(required = false) Map<String, String> body) {
        String title = body != null ? body.get("title") : null;
        return ResponseEntity.ok(conversationService.createChat(title));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long id) {
        if (conversationService.findChat(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        conversationService.deleteChat(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<ConversationService.ChatMessageDto>> getMessages(@PathVariable Long id) {
        if (conversationService.findChat(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(conversationService.getMessages(id));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ConversationService.ChatMessageDto> sendMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        if (conversationService.findChat(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String model = body.get("model");
        ConversationService.ChatMessageDto response = conversationService.sendMessage(id, question, model);
        return ResponseEntity.ok(response);
    }
}
