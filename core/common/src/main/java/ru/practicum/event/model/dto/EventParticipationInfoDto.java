package ru.practicum.event.model.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventParticipationInfoDto {
    long initiatorId;
    String state;
    int participantLimit;
    int confirmedRequests;
    boolean requestModeration;
}