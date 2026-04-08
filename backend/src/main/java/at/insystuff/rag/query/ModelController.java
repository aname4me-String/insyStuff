package at.insystuff.rag.query;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ModelController {

    @Value("${app.available-models}")
    private List<String> availableModels;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listModels() {
        return ResponseEntity.ok(Map.of("models", availableModels));
    }
}
