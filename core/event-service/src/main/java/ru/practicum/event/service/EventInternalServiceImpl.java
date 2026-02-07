package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.dto.EventParticipationInfoDto;
import ru.practicum.event.model.mapper.EventMapper;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.NotFoundException;

@Service
@RequiredArgsConstructor
public class EventInternalServiceImpl implements EventInternalService {
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    @Override
    public EventParticipationInfoDto getParticipationInfo(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с id=%d не найдено", eventId)));
        return EventMapper.toParticipationInfoDto(event);
    }

    @Transactional
    @Override
    public void incrementConfirmedRequests(Long eventId, int delta) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с id=%d не найдено", eventId)));
        event.setConfirmedRequests(event.getConfirmedRequests() + delta);
        eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean existsById(Long eventId) {
        return eventRepository.existsById(eventId);
    }
}