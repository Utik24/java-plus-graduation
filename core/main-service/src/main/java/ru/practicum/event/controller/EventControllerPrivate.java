package ru.practicum.event.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.model.dto.*;
import ru.practicum.event.service.EventService;
import ru.practicum.request.model.dto.RequestDto;

import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}/events")
@Slf4j
@RequiredArgsConstructor
public class EventControllerPrivate {
    private final EventService eventService;
    private static final String EVENT_ID_PATH = "/{eventId}";
    private final String eventIdRequests = "/{eventId}/requests";


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto postEvent(@PathVariable(name = "userId") int userId,
                                  @Valid @RequestBody NewEventDto newEventDto) {
        EventFullDto eventDto = eventService.create(newEventDto, userId);
        log.info("Создается новое событие title={}, date={}", eventDto.getTitle(), eventDto.getEventDate());
        return eventDto;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEventsByUser(@PathVariable(name = "userId") @Positive int userId,
                                               @RequestParam(name = "from", defaultValue = "0") @PositiveOrZero int from,
                                               @RequestParam(name = "size", defaultValue = "10") @PositiveOrZero int size) {
        List<EventShortDto> eventShortDtos = eventService.getAllByUser(userId, from, size);
        log.info("Получен список событий пользователя с id={}", userId);
        return eventShortDtos;
    }


    @GetMapping(EVENT_ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEventByUserAndId(@PathVariable(name = "userId") @Positive int userId,
                                            @PathVariable(name = "eventId") @Positive int eventId) {
        EventFullDto eventFullDto = eventService.getByUserAndId(userId, eventId);
        log.info("Получено событие с Id={}  пользователя с id={}", eventId, userId);
        return eventFullDto;
    }

    @PatchMapping(EVENT_ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEvent(@PathVariable(name = "userId") @Positive int userId,
                                    @PathVariable(name = "eventId") @Positive int eventId,
                                    @Valid @RequestBody UpdateEventUserRequest updateRequest) {
        EventFullDto eventFullDto = eventService.updateEvent(userId, eventId, updateRequest);
        log.info("Обновлено событие с Id={} , добавленное пользователем с id={}", eventId, userId);
        return eventFullDto;
    }


    @GetMapping(eventIdRequests)
    @ResponseStatus(HttpStatus.OK)
    public List<RequestDto> getParticipationInfo(@PathVariable(name = "userId") @Positive long userId,
                                                 @PathVariable(name = "eventId") @Positive long eventId) {
        List<RequestDto> partRequestDtoList = eventService.getParticipationInfo(userId, eventId);
        log.info("Получена информация о запросах на учатсие в событии с Id={} пользователя с id={}", eventId, userId);
        return partRequestDtoList;
    }


    @PatchMapping(eventIdRequests)
    @ResponseStatus(HttpStatus.OK)
    public EventRequestStatusUpdateResult updateEventStatus(@PathVariable(name = "userId") @Positive long userId,
                                                           @PathVariable(name = "eventId") @Positive long eventId,
                                                           @RequestBody EventRequestStatusUpdateRequest statusUpdateRequest) {
        EventRequestStatusUpdateResult updateStatusResult = eventService.updateStatus(userId, eventId, statusUpdateRequest);
        log.info("Обновлен статус события с Id={} пользователя с id={}. Статус = {}", eventId, userId, statusUpdateRequest.getStatus().toString());
        return updateStatusResult;
    }

}