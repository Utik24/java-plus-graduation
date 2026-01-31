package ru.practicum.compilation.model;

import org.springframework.stereotype.Component;
import ru.practicum.compilation.model.dto.CompilationDto;
import ru.practicum.compilation.model.dto.NewCompilationDto;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.mapper.EventMapper;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CompilationMapper {

    public CompilationDto toDto(Compilation compilation, Map<Long, Long> idViewsMap) {
        CompilationDto compilationDto = new CompilationDto();
        compilationDto.setId(compilation.getId());
        compilationDto.setPinned(compilation.isPinned());
        compilationDto.setTitle(compilation.getTitle());

        Set<Event> events = compilation.getEvents();
        if (events == null || events.size() == 0) {
            return compilationDto;
        }

        compilationDto.setEvents(compilation.getEvents().stream()
                .map(e -> EventMapper.toShortDto(e, idViewsMap.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toSet()));
        return compilationDto;
    }

    public Compilation toEntity(NewCompilationDto newCompilationDto, Set<Event> events) {
        Compilation compilation = new Compilation();
        compilation.setTitle(newCompilationDto.getTitle());
        compilation.setEvents(events);
        compilation.setPinned(newCompilationDto.isPinned());
        return compilation;
    }
}
