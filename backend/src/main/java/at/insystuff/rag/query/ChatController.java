package at.insystuff.rag.query;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final QueryService queryService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "question must not be blank"));
        }
        QueryService.ChatResponse response = queryService.answer(question);
        return ResponseEntity.ok(Map.of(
                "answer", response.answer(),
                "sources", response.sources()
        ));
    }
}
