package at.insystuff.rag.indexer;

import at.insystuff.rag.core.entity.DocumentMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IndexerController {

    private final IndexerService indexerService;

    @PostMapping("/index")
    public ResponseEntity<String> indexDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file provided");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body("Only PDF files are supported");
        }
        indexerService.indexPdf(file);
        return ResponseEntity.ok("Document indexed successfully");
    }

    @GetMapping("/documents")
    public ResponseEntity<List<DocumentMetadata>> listDocuments() {
        return ResponseEntity.ok(indexerService.listDocuments());
    }
}
