package ru.practicum.event.service;

import ru.practicum.event.model.dto.EventParticipationInfoDto;

public interface EventInternalService {

    EventParticipationInfoDto getParticipationInfo(Long eventId);

    void incrementConfirmedRequests(Long eventId, int delta);

    boolean existsById(Long eventId);
}