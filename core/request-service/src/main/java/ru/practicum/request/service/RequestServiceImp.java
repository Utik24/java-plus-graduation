package ru.practicum.request.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.dto.EventParticipationInfoDto;
import ru.practicum.exception.BadParameterException;
import ru.practicum.exception.CreateConditionException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.model.dto.RequestDto;
import ru.practicum.request.model.mapper.RequestMapper;
import ru.practicum.request.repository.RequestRepository;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImp implements RequestService {

    private final EntityManager entityManager;
    private final RequestRepository repository;
    private final EventClient eventClient;
    private final UserClient userClient;
    private final RequestRepository requestRepository;


    @Override
    public List<RequestDto> getAll(Long userId) {
        if (!userClient.existsById(userId)) {
            throw new NotFoundException(String.format("User with id = %d not found", userId));
        }
        return repository.findByRequester(userId).stream().map(RequestMapper::toRequestDto).toList();
    }

    @Override
    public RequestDto create(Long userId, Long eventId) {
        if (!userClient.existsById(userId)) {
            throw new NotFoundException(String.format("User with id = %d not found", userId));
        }
        Request duplicatedRequest = requestRepository.findByIdAndRequester(eventId, userId);
        if (duplicatedRequest != null) {
            throw new CreateConditionException(String.format("Запрос от пользователя id = %d на событие c id = %d уже существует", userId, eventId));
        }
        EventParticipationInfoDto eventInfo = eventClient.getParticipationInfo(eventId);
        /*инициатор события не может добавить запрос на участие в своём событии */
        if (eventInfo.getInitiatorId() == userId) { //если событие существует и создатель совпадает по id с пользователем
            throw new CreateConditionException("Пользователь не может создавать запрос на участие в своем событии");
        }
        /*нельзя участвовать в неопубликованном событии*/
        if (!"PUBLISHED".equals(eventInfo.getState())) {
            throw new CreateConditionException(String.format("Событие с id = %d не опубликовано", eventId));
        }
        /*нельзя участвовать при превышении лимита заявок*/
        if (eventInfo.getParticipantLimit() != 0) { //если ==0, то кол-во участников неограничено
            if (eventInfo.getConfirmedRequests() >= eventInfo.getParticipantLimit()) {
                throw new CreateConditionException(String.format("У события с id = %d достигнут лимит участников %d", eventId, eventInfo.getParticipantLimit()));
            }
        }
        Request request = new Request();
        request.setRequesterId(userId);
        request.setEventId(eventId);
        request.setCreated(LocalDateTime.now());
        if ((eventInfo.getParticipantLimit() == 0) || (!eventInfo.getRequestModeration())) {
            request.setStatus(RequestStatus.CONFIRMED);
            eventClient.incrementConfirmedRequests(eventId, 1);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }
        return RequestMapper.toRequestDto(repository.save(request));
    }

    @Override
    public RequestDto cancelRequest(Long userId, Long requestId) {
        if (!userClient.existsById(userId)) {
            throw new NotFoundException(String.format("User with id = %d not found", userId));
        }
        Request request = repository.findById(requestId).orElseThrow(() -> new NotFoundException(String.format("Request with id = %d not found", requestId)));
        repository.updateToCanceled(requestId);
        repository.flush();
        entityManager.clear();
        return RequestMapper.toRequestDto(repository.findById(requestId).get());
    }

    @Override
    public List<RequestDto> getAllRequestsEventId(Long eventId) {
        if (eventId < 0) {
            throw new BadParameterException("Id события должен быть больше 0");
        }

        List<Request> partRequests = repository.findAllByEventId(eventId); //запрашиваем все запросы на событие
        if (partRequests == null || partRequests.isEmpty()) { //если запросов нет, возвращаем пустой список
            return new ArrayList<>();
        }
        /*преобразуем в DTO и возвращаем*/
        return partRequests.stream()
                .map(RequestMapper::toRequestDto)
                .collect(Collectors.toList());
    }

    public void updateEventRequests(Long eventId, List<RequestDto> requestDtoList) {
        if (requestDtoList == null || requestDtoList.isEmpty()) {
            return;
        }
        eventClient.getParticipationInfo(eventId);
        Map<Long, Request> existing = requestRepository.findAllById(requestDtoList.stream()
                        .map(RequestDto::getId)
                        .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Request::getId, r -> r));

        List<Request> updated = requestDtoList.stream()
                .map(dto -> {
                    Request request = existing.getOrDefault(dto.getId(), RequestMapper.toRequest(dto));
                    request.setEventId(eventId);
                    request.setRequesterId(dto.getRequester());
                    request.setStatus(dto.getStatus());
                    request.setCreated(LocalDateTime.parse(dto.getCreated(), RequestMapper.TIME_FORMAT));
                    return request;
                })
                .collect(Collectors.toList());

        requestRepository.saveAll(updated);
    }

    @Override
    @Transactional
    public void updateAll(List<RequestDto> requestDtoList, Event event) {
        if (event == null) {
            throw new NotFoundException("Event is required to update requests");
        }
        updateEventRequests(event.getId(), requestDtoList);
    }


    @Override
    @Transactional
    public void update(RequestDto prDto, Event event) {
        if (event == null) {
            throw new NotFoundException("Event is required to update request");
        }
        updateEventRequests(event.getId(), List.of(prDto));
    }
}
