package ru.practicum.event.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.practicum.event.model.Location;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewEventDto {
    @NotBlank
    @Size(min = 20, max = 2000)
    String annotation;
    @NotNull
    long category;
    @NotBlank
    @Size(min = 20, max = 7000)
    String description;
    @NotBlank
    String eventDate;
    @NotNull
    Location location;
    boolean paid = false;
    int participantLimit = 0;
    boolean requestModeration = true;
    @Size(min = 3, max = 120)
    String title;
}
