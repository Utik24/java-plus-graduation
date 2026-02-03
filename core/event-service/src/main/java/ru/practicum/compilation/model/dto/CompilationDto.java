package ru.practicum.compilation.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.practicum.event.model.dto.EventShortDto;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompilationDto {
    @NotNull
    Long id;
    @NotBlank
    String title;
    boolean pinned;
    @NotNull
    Set<EventShortDto> events = new HashSet<>();
}
