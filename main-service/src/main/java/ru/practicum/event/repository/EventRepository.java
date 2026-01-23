package ru.practicum.event.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.category.model.Category;
import ru.practicum.event.model.Event;

import java.util.List;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long> {
    @Query("SELECT e " +
            "FROM Event AS e " +
            "WHERE e.category.id = ?1")
    List<Event> findByCategoryId(long catId);


    @Query("SELECT e " +
            "FROM Event as e " +
            "WHERE initiator.id = ?1")
    List<Event> getAllByUser(long userId, PageRequest page);

    @Query("SELECT e " +
            "FROM Event as e " +
            "WHERE id = ?1 " +
            "AND initiator.id = ?2")
    Event getByIdAndUserId(long eventId, long userId);

    List<Event> findByIdIn(Set<Long> eventIds);

    @Query("SELECT e " +
            "FROM Event as e " +
            "JOIN FETCH e.initiator " +
            "WHERE e.id in ?1 ")
    List<Event> findEventsWIthUsersByIdSet(Set<Long> eventIds);

    boolean existsByCategory(Category category);
}
