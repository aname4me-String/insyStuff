package at.insystuff.rag.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatisticsController {

    private final BenchmarkService benchmarkService;

    @GetMapping
    public ResponseEntity<BenchmarkService.StatsResponse> getStats(
            @RequestParam(required = false) String vectorStoreTypes,
            @RequestParam(required = false) String models,
            @RequestParam(defaultValue = "50") int recentLimit) {

        List<String> vsFilter = parseCommaSeparated(vectorStoreTypes);
        List<String> modelFilter = parseCommaSeparated(models);
        int limit = Math.max(1, Math.min(recentLimit, 1000));

        return ResponseEntity.ok(benchmarkService.computeStats(vsFilter, modelFilter, limit));
    }

    private List<String> parseCommaSeparated(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
