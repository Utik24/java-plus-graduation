package ru.practicum.request.model.mapper;

import ru.practicum.event.model.Event;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.dto.RequestDto;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RequestMapper {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static RequestDto toRequestDto(Request request) {
        RequestDto dto = new RequestDto();
        dto.setCreated(request.getCreated().format(TIME_FORMAT));
        dto.setEvent(request.getEvent().getId());
        dto.setId(request.getId());
        dto.setRequester(request.getRequester().getId());
        dto.setStatus(request.getStatus());
        return dto;
    }

    public static Request toRequest(RequestDto dto, Event event, User user) {
        Request pr = new Request();
        pr.setId(dto.getId());
        pr.setCreated(LocalDateTime.parse(dto.getCreated(), TIME_FORMAT));
        pr.setEvent(event);
        pr.setRequester(user);
        pr.setStatus(dto.getStatus());
        return pr;
    }
}
