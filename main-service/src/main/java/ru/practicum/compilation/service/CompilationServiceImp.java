package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.model.CompilationMapper;
import ru.practicum.compilation.model.dto.CompilationDto;
import ru.practicum.compilation.model.dto.NewCompilationDto;
import ru.practicum.compilation.model.dto.UpdateCompilationRequest;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.service.EventService;
import ru.practicum.exception.BadParameterException;
import ru.practicum.exception.DataConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationServiceImp implements CompilationService {
    private final EventService eventService;
    private final CompilationRepository compilationRepository;

    @Override
    public CompilationDto create(NewCompilationDto newCompilationDto) {
        try {
            validateNewCompilationDto(newCompilationDto);
            Set<Event> eventSet = getEventsForCompilation(newCompilationDto.getEvents());
            Compilation compilation = createAndSaveCompilation(newCompilationDto, eventSet);
            return CompilationMapper.toDto(compilation);
        } catch (DataAccessException e) {
            log.error("Access error", e);
            throw new DataConflictException("Access error");
        } catch (Exception e) {
            log.error("Database error", e);
            throw new DataConflictException("Database error");
        }
    }

    @Override
    public void deleteById(long compId) {
        compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format("Сompilation with id=%d not found", compId)));
        compilationRepository.deleteById(compId);
    }

    @Override
    @Transactional
    public CompilationDto update(long compId, UpdateCompilationRequest updateRequest) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format("Compilation with id=%d not found", compId)));
        Set<Long> eventIds = updateRequest.getEvents();
        Set<Event> eventsSet;
        if (eventIds == null || eventIds.isEmpty()) {
            eventsSet = new HashSet<>();
        } else {
            eventsSet = eventService.getEventsByIds(eventIds);
        }
        Boolean pinned = updateRequest.getPinned();
        if (pinned != null) {
            compilation.setPinned(pinned);
        }
        String title = updateRequest.getTitle();
        if (title != null) {
            compilation.setTitle(title);
        }
        compilation.setEvents(eventsSet);
        compilation = compilationRepository.save(compilation);
        return CompilationMapper.toDto(compilation);
    }

    public List<CompilationDto> getAllComps(boolean pinned, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Compilation> compilations = compilationRepository.findByPinned(pinned, page);
        return compilations.stream()
                .map(CompilationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompById(long compId) {

        if (compId <= 0) {
            throw new BadParameterException("Id value is less than 1");
        }

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format("Compilation with id=%d not found", compId)));
        return CompilationMapper.toDto(compilation);
    }

    private void validateNewCompilationDto(NewCompilationDto newCompilationDto) {
        if (newCompilationDto == null) {
            throw new IllegalArgumentException("newCompilationDto is null");
        }
        if (newCompilationDto.getTitle() == null || newCompilationDto.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is null");
        }
    }

    private Set<Event> getEventsForCompilation(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Event> eventsWithInitiators = eventService.getEventsByIds(eventIds);
        return new HashSet<>(eventsWithInitiators);
    }

    private Compilation createAndSaveCompilation(NewCompilationDto newCompilationDto, Set<Event> events) {
        Compilation compilation = CompilationMapper.toEntity(newCompilationDto, events);
        validateCompilationBeforeSave(compilation);
        return compilationRepository.save(compilation);
    }

    private void validateCompilationBeforeSave(Compilation compilation) {
        if (compilation.getTitle() == null || compilation.getTitle().isBlank()) {
            throw new IllegalArgumentException("Заголовок подборки обязателен");
        }
    }
}