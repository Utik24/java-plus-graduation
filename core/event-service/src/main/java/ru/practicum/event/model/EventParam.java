package ru.practicum.event.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventParam {
    Long userId;
    Long eventId;

    String text;
    List<Long> categories;
    Boolean paid;
    LocalDateTime rangeStart;
    LocalDateTime rangeEnd;

    Boolean onlyAvailable;
    String sort;

    List<Long> users;
    List<String> states;

    Integer from;
    Integer size;

}
