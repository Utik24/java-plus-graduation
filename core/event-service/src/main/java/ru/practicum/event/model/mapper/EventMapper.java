package ru.practicum.event.model.mapper;

import ru.practicum.category.model.Category;
import ru.practicum.category.model.mapper.CategoryMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.dto.EventFullDto;
import ru.practicum.event.model.dto.EventParticipationInfoDto;
import ru.practicum.event.model.dto.EventShortDto;
import ru.practicum.event.model.dto.NewEventDto;
import ru.practicum.user.model.User;
import ru.practicum.user.model.mapper.UserMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EventMapper {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Event toEvent(NewEventDto newEventDto, Category category, User user) {
        Event event = new Event();

        event.setAnnotation(newEventDto.getAnnotation());
        event.setCategory(category);
        event.setCreatedOn(LocalDateTime.now());
        event.setDescription(newEventDto.getDescription());
        event.setEventDate(LocalDateTime.parse(newEventDto.getEventDate(), TIME_FORMAT));
        event.setInitiator(user);
        event.setLocation(newEventDto.getLocation());

        event.setPaid(newEventDto.isPaid());
        event.setParticipantLimit(newEventDto.getParticipantLimit());
        event.setRequestModeration(newEventDto.isRequestModeration());
        event.setTitle(newEventDto.getTitle());

        return event;
    }

    public static Event toEvent(EventFullDto eventFullDto, User user) {
        Event event = new Event();

        event.setId(eventFullDto.getId());
        event.setAnnotation(eventFullDto.getAnnotation());
        event.setCategory(CategoryMapper.toCategory(eventFullDto.getCategory()));
        event.setConfirmedRequests(eventFullDto.getConfirmedRequests());
        if (eventFullDto.getCreatedOn() != null && !eventFullDto.getCreatedOn().isBlank()) {
            event.setCreatedOn(LocalDateTime.parse(eventFullDto.getCreatedOn(), TIME_FORMAT));
        } else {
            event.setCreatedOn(LocalDateTime.now());
        }        event.setDescription(eventFullDto.getDescription());
        event.setEventDate(LocalDateTime.parse(eventFullDto.getEventDate(), TIME_FORMAT));
        event.setInitiator(user);
        event.setLocation(eventFullDto.getLocation());

        event.setPaid(eventFullDto.isPaid());
        event.setParticipantLimit(eventFullDto.getParticipantLimit());
        if (eventFullDto.getPublishedOn() != null && !eventFullDto.getPublishedOn().isBlank()) {
            event.setPublishedOn(LocalDateTime.parse(eventFullDto.getPublishedOn(), TIME_FORMAT));
        }        event.setRequestModeration(eventFullDto.isRequestModeration());
        event.setState(eventFullDto.getState());
        event.setTitle(eventFullDto.getTitle());

        return event;
    }


    public static EventShortDto toShortDto(Event event, long views) {
        EventShortDto shortDto = new EventShortDto();

        shortDto.setId(event.getId());
        shortDto.setAnnotation(event.getAnnotation());
        shortDto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
        shortDto.setConfirmedRequests(event.getConfirmedRequests());
        shortDto.setEventDate(event.getEventDate().format(TIME_FORMAT));
        shortDto.setInitiator(UserMapper.toShortDto(event.getInitiator()));
        shortDto.setPaid(event.isPaid());
        shortDto.setTitle(event.getTitle());
        shortDto.setViews(views);

        return shortDto;
    }


    public static EventFullDto toFullDto(Event event, long views) {
        EventFullDto eventFullDto = new EventFullDto();

        eventFullDto.setId(event.getId());
        eventFullDto.setAnnotation(event.getAnnotation());
        eventFullDto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
        eventFullDto.setConfirmedRequests(event.getConfirmedRequests());
        eventFullDto.setCreatedOn(event.getCreatedOn().format(TIME_FORMAT));
        eventFullDto.setDescription(event.getDescription());
        eventFullDto.setEventDate(event.getEventDate().format(TIME_FORMAT));
        eventFullDto.setInitiator(UserMapper.toShortDto(event.getInitiator()));
        eventFullDto.setLocation(event.getLocation());
        eventFullDto.setPaid((event.isPaid()));
        eventFullDto.setParticipantLimit(event.getParticipantLimit());
        if (event.getPublishedOn() != null) {
            eventFullDto.setPublishedOn(event.getPublishedOn().format(TIME_FORMAT));
        }        eventFullDto.setRequestModeration(event.isRequestModeration());
        eventFullDto.setState(event.getState());
        eventFullDto.setTitle(event.getTitle());
        eventFullDto.setViews(views);

        return eventFullDto;
    }

    public static EventParticipationInfoDto toParticipationInfoDto(Event event) {
        EventParticipationInfoDto dto = new EventParticipationInfoDto();
        dto.setInitiatorId(event.getInitiator().getId());
        dto.setState(event.getState().name());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setConfirmedRequests(event.getConfirmedRequests());
        dto.setRequestModeration(event.isRequestModeration());
        return dto;
    }
}
