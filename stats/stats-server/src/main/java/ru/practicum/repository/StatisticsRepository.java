package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.model.Statistics;

import java.time.LocalDateTime;
import java.util.List;


public interface StatisticsRepository extends JpaRepository<Statistics, Long> {

    @Query(value = """
            SELECT s.app AS app, s.uri AS uri, COUNT(*) AS hits
            FROM statistics s
            WHERE s.call_time BETWEEN :start AND :end
            GROUP BY s.app, s.uri
            ORDER BY hits DESC
            """, nativeQuery = true)
    List<ViewStats> findStats(LocalDateTime start, LocalDateTime end);

    @Query(value = """
            SELECT s.app AS app, s.uri AS uri, COUNT(DISTINCT s.ip) AS hits
            FROM statistics s
            WHERE s.call_time BETWEEN :start AND :end
            GROUP BY s.app, s.uri
            ORDER BY hits DESC
            """, nativeQuery = true)
    List<ViewStats> findUniqueStats(LocalDateTime start, LocalDateTime end);

    @Query(value = """
            SELECT s.app AS app, s.uri AS uri, COUNT(*) AS hits
            FROM statistics s
            WHERE s.call_time BETWEEN :start AND :end
              AND s.uri IN (:uris)
            GROUP BY s.app, s.uri
            ORDER BY hits DESC
            """, nativeQuery = true)
    List<ViewStats> findStatsByUris(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query(value = """
            SELECT s.app AS app, s.uri AS uri, COUNT(DISTINCT s.ip) AS hits
            FROM statistics s
            WHERE s.call_time BETWEEN :start AND :end
              AND s.uri IN (:uris)
            GROUP BY s.app, s.uri
            ORDER BY hits DESC
            """, nativeQuery = true)
    List<ViewStats> findUniqueStatsByUris(LocalDateTime start, LocalDateTime end, List<String> uris);
}
