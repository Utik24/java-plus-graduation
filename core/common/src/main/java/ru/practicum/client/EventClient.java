package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.event.model.dto.EventParticipationInfoDto;

@FeignClient(name = "event-service")
public interface EventClient {
    @GetMapping("/internal/events/{eventId}/participation-info")
    EventParticipationInfoDto getParticipationInfo(@PathVariable("eventId") Long eventId);

    @PatchMapping("/internal/events/{eventId}/confirmed-requests")
    void incrementConfirmedRequests(@PathVariable("eventId") Long eventId,
                                    @RequestParam("delta") int delta);

    @GetMapping("/internal/events/{eventId}/exists")
    boolean existsById(@PathVariable("eventId") Long eventId);
}