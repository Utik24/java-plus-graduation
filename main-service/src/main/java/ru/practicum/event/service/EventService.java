package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;

import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventParam;
import ru.practicum.event.model.dto.*;
import ru.practicum.request.model.dto.RequestDto;

import java.util.List;
import java.util.Set;

public interface EventService {
    List<EventShortDto> getEvents(EventParam p);

    List<EventFullDto> getEventsAdmin(EventParam p);

    EventFullDto getEvent(int eventId,
                          HttpServletRequest request);

    EventFullDto create(NewEventDto newEventDto, int userId);

    List<EventShortDto> getAllByUser(int userId, int from, int size);

    EventFullDto getByUserAndId(int userId, int eventId);

    EventFullDto updateEvent(int userId, int eventId, UpdateEventUserRequest updateRequest);

    List<RequestDto> getParticipationInfo(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest statusUpdateRequest);

    EventFullDto updateAdminEvent(long eventId, UpdateEventAdminRequest adminRequest);

    Set<Event> getEventsByIds(Set<Long> eventIds);
}
