package at.insystuff.rag.statistics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestBenchmarkRepository extends JpaRepository<RequestBenchmark, Long> {

    List<RequestBenchmark> findTop200ByOrderByTimestampDesc();

    List<RequestBenchmark> findTop1000ByOrderByTimestampDesc();
}
