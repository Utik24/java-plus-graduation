package ru.practicum.event.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.practicum.category.model.dto.CategoryDto;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.Location;
import ru.practicum.user.model.dto.UserShortDto;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventFullDto {
    long id;
    @NotBlank
    String annotation;
    @NotNull
    CategoryDto category;
    int confirmedRequests;
    @NotBlank
    String createdOn;
    @NotBlank
    String description;
    @NotBlank
    String eventDate;
    @NotNull
    UserShortDto initiator;
    @NotNull
    Location location;
    boolean paid;
    int participantLimit = 0;
    @NotBlank
    String publishedOn;
    boolean requestModeration = false;
    EventState state = EventState.PENDING;
    @NotBlank
    String title;
    long views;
}