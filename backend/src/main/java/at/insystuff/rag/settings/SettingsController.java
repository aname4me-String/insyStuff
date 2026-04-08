package at.insystuff.rag.settings;

import at.insystuff.rag.vectorstore.VectorStoreRouter;
import at.insystuff.rag.vectorstore.VectorStoreType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final VectorStoreRouter vectorStoreRouter;

    @GetMapping("/vectorstore/types")
    public ResponseEntity<List<String>> getVectorStoreTypes() {
        List<String> types = Arrays.stream(VectorStoreType.values())
                .map(VectorStoreType::name)
                .toList();
        return ResponseEntity.ok(types);
    }

    @GetMapping("/vectorstore")
    public ResponseEntity<Map<String, String>> getActiveVectorStore() {
        return ResponseEntity.ok(Map.of("activeVectorStore", vectorStoreRouter.getActiveType().name()));
    }

    @PutMapping("/vectorstore")
    public ResponseEntity<Map<String, String>> setActiveVectorStore(@RequestBody Map<String, String> body) {
        String typeName = body.get("activeVectorStore");
        if (typeName == null || typeName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        VectorStoreType type;
        try {
            type = VectorStoreType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        vectorStoreRouter.setActiveType(type);
        return ResponseEntity.ok(Map.of("activeVectorStore", type.name()));
    }
}
