package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.model.dto.EventParticipationInfoDto;
import ru.practicum.event.service.EventInternalService;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class EventInternalController {
    private final EventInternalService eventInternalService;

    @GetMapping("/{eventId}/participation-info")
    public EventParticipationInfoDto getParticipationInfo(@PathVariable("eventId") Long eventId) {
        return eventInternalService.getParticipationInfo(eventId);
    }


    @PatchMapping("/{eventId}/confirmed-requests")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void incrementConfirmedRequests(@PathVariable("eventId") Long eventId,
                                           @RequestParam("delta") int delta) {
        eventInternalService.incrementConfirmedRequests(eventId, delta);
    }

    @GetMapping("/{eventId}/exists")
    public boolean existsById(@PathVariable("eventId") Long eventId) {
        return eventInternalService.existsById(eventId);
    }
}