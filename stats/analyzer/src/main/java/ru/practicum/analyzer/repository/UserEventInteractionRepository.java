package ru.practicum.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.UserEventId;
import ru.practicum.analyzer.model.UserEventInteractionEntity;

import java.util.List;

public interface UserEventInteractionRepository extends JpaRepository<UserEventInteractionEntity, UserEventId> {
    List<UserEventInteractionEntity> findByIdUserId(Long userId);

    List<UserEventInteractionEntity> findByIdUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    boolean existsByIdUserIdAndIdEventId(Long userId, Long eventId);

    @Query("select u.id.eventId as eventId, sum(u.weight) as totalWeight "
            + "from UserEventInteractionEntity u "
            + "where u.id.eventId in :eventIds group by u.id.eventId")
    List<InteractionSum> sumWeightsByEventIds(@Param("eventIds") List<Long> eventIds);

    interface InteractionSum {
        Long getEventId();

        Double getTotalWeight();
    }
}