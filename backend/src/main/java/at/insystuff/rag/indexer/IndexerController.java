package at.insystuff.rag.indexer;

import at.insystuff.rag.core.entity.DocumentMetadata;
import at.insystuff.rag.vectorstore.VectorStoreRouter;
import at.insystuff.rag.vectorstore.VectorStoreType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IndexerController {

    private final IndexerService indexerService;
    private final VectorStoreRouter vectorStoreRouter;

    @PostMapping("/index")
    public ResponseEntity<String> indexDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "vectorStoreType", required = false) String vectorStoreType) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file provided");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body("Only PDF files are supported");
        }
        if (vectorStoreType != null && !vectorStoreType.isBlank()) {
            try {
                VectorStoreType vsType = VectorStoreType.valueOf(vectorStoreType.toUpperCase());
                vectorStoreRouter.setActiveType(vsType);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Unknown vectorStoreType: " + vectorStoreType);
            }
        }
        indexerService.indexPdf(file);
        return ResponseEntity.ok("Document indexed successfully");
    }

    @GetMapping("/documents")
    public ResponseEntity<List<DocumentMetadata>> listDocuments() {
        return ResponseEntity.ok(indexerService.listDocuments());
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        if (indexerService.deleteDocument(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/documents/{id}")
    public ResponseEntity<DocumentMetadata> renameDocument(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String newName = body.get("fileName");
        if (newName == null || newName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return indexerService.renameDocument(id, newName.trim())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
