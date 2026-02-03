package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.dto.EventParticipationInfoDto;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.NotFoundException;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class EventInternalController {
    private final EventRepository eventRepository;

    @GetMapping("/{eventId}/participation-info")
    public EventParticipationInfoDto getParticipationInfo(@PathVariable("eventId") Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с id=%d не найдено", eventId)));
        EventParticipationInfoDto dto = new EventParticipationInfoDto();
        dto.setInitiatorId(event.getInitiator().getId());
        dto.setState(event.getState().name());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setConfirmedRequests(event.getConfirmedRequests());
        dto.setRequestModeration(event.isRequestModeration());
        return dto;
    }

    @PatchMapping("/{eventId}/confirmed-requests")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void incrementConfirmedRequests(@PathVariable("eventId") Long eventId,
                                           @RequestParam("delta") int delta) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с id=%d не найдено", eventId)));
        event.setConfirmedRequests(event.getConfirmedRequests() + delta);
        eventRepository.save(event);
    }

    @GetMapping("/{eventId}/exists")
    public boolean existsById(@PathVariable("eventId") Long eventId) {
        return eventRepository.existsById(eventId);
    }
}