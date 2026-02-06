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
import ru.practicum.client.StatsClient;
import ru.practicum.exception.BadParameterException;
import ru.practicum.exception.DataConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.client.UserClient;
import ru.practicum.user.model.dto.UserRequest;
import ru.practicum.user.model.dto.UserShortDto;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationServiceImp implements CompilationService {
    private final EventService eventService;
    private final CompilationRepository compilationRepository;
    private final StatsClient statsClient;
    private final UserClient userClient;

    @Override
    public CompilationDto create(NewCompilationDto newCompilationDto) {
        try {
            validateNewCompilationDto(newCompilationDto);
            Set<Event> eventSet = getEventsForCompilation(newCompilationDto.getEvents());
            Compilation compilation = createAndSaveCompilation(newCompilationDto, eventSet);
            return mapCompilationWithViews(compilation);
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
        if (eventIds != null) {
            Set<Event> eventsSet = eventIds.isEmpty() ? new HashSet<>() : eventService.getEventsByIds(eventIds);
            compilation.setEvents(eventsSet);
        }
        Boolean pinned = updateRequest.getPinned();
        if (pinned != null) {
            compilation.setPinned(pinned);
        }
        String title = updateRequest.getTitle();
        if (title != null) {
            compilation.setTitle(title);
        }
        compilation = compilationRepository.save(compilation);
        return mapCompilationWithViews(compilation);
    }

    public List<CompilationDto> getAllComps(boolean pinned, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Compilation> compilations = compilationRepository.findByPinned(pinned, page);
        Map<Long, Long> idViewsMap = getViewsMapForCompilations(compilations);
        Set<Long> userIds = compilations.stream()
                .map(Compilation::getEvents)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());
        Map<Long, UserShortDto> initiators = getUserShortsByIds(userIds);
        return compilations.stream()
                .map(compilation -> CompilationMapper.toDto(compilation, idViewsMap, initiators))
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompById(long compId) {

        if (compId <= 0) {
            throw new BadParameterException("Id value is less than 1");
        }

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format("Compilation with id=%d not found", compId)));
        return mapCompilationWithViews(compilation);
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

    private CompilationDto mapCompilationWithViews(Compilation compilation) {
        Map<Long, Long> idViewsMap = getViewsMap(compilation.getEvents());
        Map<Long, UserShortDto> initiators = getUserShorts(compilation.getEvents());
        return CompilationMapper.toDto(compilation, idViewsMap, initiators);
    }

    private Map<Long, Long> getViewsMap(Set<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }
        return statsClient.getMapIdViews(events.stream()
                .map(Event::getId)
                .collect(Collectors.toList()));
    }

    private Map<Long, Long> getViewsMapForCompilations(List<Compilation> compilations) {
        List<Long> eventIds = compilations.stream()
                .map(Compilation::getEvents)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .map(Event::getId)
                .distinct()
                .collect(Collectors.toList());
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return statsClient.getMapIdViews(eventIds);
    }
    private Map<Long, UserShortDto> getUserShorts(Set<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }
        Set<Long> userIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());
        return getUserShortsByIds(userIds);
    }

    private Map<Long, UserShortDto> getUserShortsByIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, UserShortDto> result = new HashMap<>();
        try {
            List<UserRequest> users = userClient.getByIds(new ArrayList<>(userIds));
            if (users != null) {
                for (UserRequest user : users) {
                    UserShortDto dto = new UserShortDto();
                    dto.setId(user.getId());
                    dto.setName(user.getName());
                    result.put(user.getId(), dto);
                }
            }
        } catch (RuntimeException ex) {
            result.clear();
        }
        for (Long userId : userIds) {
            result.computeIfAbsent(userId, this::getUserShort);
        }
        return result;
    }

    private UserShortDto getUserShort(long userId) {
        UserRequest userRequest;
        try {
            userRequest = userClient.getById(userId);
        } catch (RuntimeException ex) {
            userRequest = null;
        }
        if (userRequest == null) {
            throw new NotFoundException(String.format("User with id=%d not found", userId));
        }
        UserShortDto dto = new UserShortDto();
        dto.setId(userRequest.getId());
        dto.setName(userRequest.getName());
        return dto;
    }
}