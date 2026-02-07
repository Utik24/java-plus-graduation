package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.analyzer.model.EventPairId;
import ru.practicum.analyzer.model.EventSimilarityEntity;

import java.util.List;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarityEntity, EventPairId> {
    List<EventSimilarityEntity> findByIdEventAOrIdEventB(Long eventA, Long eventB);

    EventSimilarityEntity findByIdEventAAndIdEventB(Long eventA, Long eventB);
}