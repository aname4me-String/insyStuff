package at.insystuff.rag.statistics;

import at.insystuff.rag.vectorstore.VectorStoreRouter;
import com.sun.management.OperatingSystemMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LongSummaryStatistics;

@Service
@RequiredArgsConstructor
@Slf4j
public class BenchmarkService {

    private final RequestBenchmarkRepository repository;
    private final VectorStoreRouter vectorStoreRouter;

    // -----------------------------------------------------------------------
    // Recording
    // -----------------------------------------------------------------------

    public void record(String question, String model,
                       long vectorSearchMs, long totalResponseMs,
                       int tokenCount, int sourceCount) {
        try {
            RequestBenchmark b = new RequestBenchmark();
            b.setTimestamp(OffsetDateTime.now());
            b.setQuestion(question != null && question.length() > 300
                    ? question.substring(0, 300) : question);
            b.setModel(model);
            b.setVectorStoreType(vectorStoreRouter.getActiveType().name());
            b.setVectorSearchMs(vectorSearchMs);
            b.setTotalResponseMs(totalResponseMs);
            b.setTokenCount(tokenCount);
            b.setSourceCount(sourceCount);
            b.setRamUsedMb(currentRamUsedMb());
            b.setCpuLoadPercent(currentCpuLoadPercent());
            repository.save(b);
        } catch (Exception e) {
            log.warn("Failed to record benchmark", e);
        }
    }

    // -----------------------------------------------------------------------
    // Aggregation
    // -----------------------------------------------------------------------

    /** Compute stats with optional filters. Empty lists mean "no filter" (all values pass). */
    public StatsResponse computeStats(List<String> vectorStoreTypes, List<String> models, int recentLimit) {
        List<RequestBenchmark> all = repository.findTop1000ByOrderByTimestampDesc();
        boolean filterVs = !vectorStoreTypes.isEmpty();
        boolean filterModel = !models.isEmpty();

        List<RequestBenchmark> filtered = all.stream()
                .filter(b -> !filterVs || vectorStoreTypes.contains(b.getVectorStoreType()))
                .filter(b -> !filterModel || models.contains(b.getModel()))
                .toList();

        int total = (int) repository.count();
        if (filtered.isEmpty()) {
            return new StatsResponse(total,
                    vectorStoreRouter.getActiveType().name(),
                    empty(), empty(), empty(), empty(), empty(), empty(),
                    List.of());
        }

        MetricStats vectorSearch = computeLong(filtered, b -> b.getVectorSearchMs() != null ? b.getVectorSearchMs() : 0L);
        MetricStats totalResponse = computeLong(filtered, b -> b.getTotalResponseMs() != null ? b.getTotalResponseMs() : 0L);
        MetricStats tokens = computeInt(filtered, b -> b.getTokenCount() != null ? (long) b.getTokenCount() : 0L);
        MetricStats sources = computeInt(filtered, b -> b.getSourceCount() != null ? (long) b.getSourceCount() : 0L);
        MetricStats ram = computeLong(filtered, b -> b.getRamUsedMb() != null ? b.getRamUsedMb() : 0L);
        MetricStats cpu = computeDouble(filtered, b -> b.getCpuLoadPercent() != null ? b.getCpuLoadPercent() : 0.0);

        int limit = Math.min(recentLimit, filtered.size());
        List<RecentRequest> recent = filtered.subList(0, limit).stream()
                .map(this::toDto)
                .toList();

        return new StatsResponse(total,
                vectorStoreRouter.getActiveType().name(),
                vectorSearch, totalResponse, tokens, sources, ram, cpu,
                recent);
    }

    public StatsResponse computeStats() {
        return computeStats(List.of(), List.of(), 50);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private interface LongExtractor {
        long extract(RequestBenchmark b);
    }

    private interface DoubleExtractor {
        double extract(RequestBenchmark b);
    }

    private MetricStats computeLong(List<RequestBenchmark> data, LongExtractor extractor) {
        List<Long> values = data.stream()
                .map(b -> extractor.extract(b))
                .sorted()
                .toList();
        double avg = values.stream().mapToLong(Long::longValue).average().orElse(0);
        double median = medianOfLongs(values);
        long min = values.stream().mapToLong(Long::longValue).min().orElse(0);
        long max = values.stream().mapToLong(Long::longValue).max().orElse(0);
        return new MetricStats(round2(avg), round2(median), (double) min, (double) max);
    }

    private MetricStats computeInt(List<RequestBenchmark> data, LongExtractor extractor) {
        return computeLong(data, extractor);
    }

    private MetricStats computeDouble(List<RequestBenchmark> data, DoubleExtractor extractor) {
        List<Double> values = data.stream()
                .map(b -> extractor.extract(b))
                .sorted()
                .toList();
        double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double median = medianOfDoubles(values);
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        return new MetricStats(round2(avg), round2(median), round2(min), round2(max));
    }

    private double medianOfLongs(List<Long> sorted) {
        if (sorted.isEmpty()) return 0;
        int n = sorted.size();
        if (n % 2 == 1) return sorted.get(n / 2);
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    private double medianOfDoubles(List<Double> sorted) {
        if (sorted.isEmpty()) return 0;
        int n = sorted.size();
        if (n % 2 == 1) return sorted.get(n / 2);
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private MetricStats empty() {
        return new MetricStats(0, 0, 0, 0);
    }

    private RecentRequest toDto(RequestBenchmark b) {
        return new RecentRequest(
                b.getId(),
                b.getTimestamp() != null ? b.getTimestamp().toString() : null,
                b.getQuestion(),
                b.getModel(),
                b.getVectorStoreType(),
                b.getVectorSearchMs() != null ? b.getVectorSearchMs() : 0L,
                b.getTotalResponseMs() != null ? b.getTotalResponseMs() : 0L,
                b.getTokenCount() != null ? b.getTokenCount() : 0,
                b.getSourceCount() != null ? b.getSourceCount() : 0,
                b.getRamUsedMb() != null ? b.getRamUsedMb() : 0L,
                b.getCpuLoadPercent() != null ? b.getCpuLoadPercent() : 0.0
        );
    }

    private long currentRamUsedMb() {
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        return used / (1024 * 1024);
    }

    private double currentCpuLoadPercent() {
        try {
            java.lang.management.OperatingSystemMXBean base =
                    ManagementFactory.getOperatingSystemMXBean();
            if (base instanceof OperatingSystemMXBean osBean) {
                double load = osBean.getCpuLoad();
                if (load >= 0) return round2(load * 100.0);
            }
        } catch (Exception e) {
            log.debug("Could not read CPU load", e);
        }
        return -1.0;
    }

    // -----------------------------------------------------------------------
    // DTOs (records)
    // -----------------------------------------------------------------------

    public record MetricStats(double avg, double median, double min, double max) {}

    public record RecentRequest(
            Long id,
            String timestamp,
            String question,
            String model,
            String vectorStoreType,
            long vectorSearchMs,
            long totalResponseMs,
            int tokenCount,
            int sourceCount,
            long ramUsedMb,
            double cpuLoadPercent
    ) {}

    public record StatsResponse(
            int totalRequests,
            String activeVectorStore,
            MetricStats vectorSearchMs,
            MetricStats totalResponseMs,
            MetricStats tokenCount,
            MetricStats sourceCount,
            MetricStats ramUsedMb,
            MetricStats cpuLoadPercent,
            List<RecentRequest> recentRequests
    ) {}
}
