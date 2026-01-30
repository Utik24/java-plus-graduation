package ru.practicum.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.model.EventParam;
import ru.practicum.event.model.dto.EventFullDto;
import ru.practicum.event.model.dto.UpdateEventAdminRequest;
import ru.practicum.event.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/events")
@Slf4j
@RequiredArgsConstructor
public class EventControllerAdmin {
    private final EventService eventService;
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String EVENT_ID_PATH = "/{eventId}";


    @PatchMapping(EVENT_ID_PATH)
    public EventFullDto updateEvent(@PathVariable(name = "eventId") @Positive int eventId,
                                   @Valid @RequestBody UpdateEventAdminRequest adminRequest) {
        EventFullDto eventFullDto = eventService.updateAdminEvent(eventId, adminRequest);
        log.info("Админ обновил событие с Id={}", eventId);
        return eventFullDto;
    }

    @GetMapping
    public List<EventFullDto> getEvents(@RequestParam(name = "users", required = false) List<Integer> users,
                                           @RequestParam(name = "states", required = false) List<String> states,
                                           @RequestParam(name = "categories", required = false) List<Long> categories,
                                           @RequestParam(name = "rangeStart", required = false) @DateTimeFormat(pattern = DATE_TIME_PATTERN) LocalDateTime rangeStart,
                                           @RequestParam(name = "rangeEnd", required = false) @DateTimeFormat(pattern = DATE_TIME_PATTERN) LocalDateTime rangeEnd,
                                           @RequestParam(name = "from", defaultValue = "0") @PositiveOrZero int from,
                                           @RequestParam(name = "size", defaultValue = "10") @Positive int size) {
        EventParam p = EventParam.builder()
                .categories(categories)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .from(from)
                .size(size)
                .build();

        List<EventFullDto> events = eventService.getEventsAdmin(p);
        log.info("Выполнен поиск событий администратором");
        return events;
    }

}
