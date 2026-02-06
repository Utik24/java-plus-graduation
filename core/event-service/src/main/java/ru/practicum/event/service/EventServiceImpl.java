package ru.practicum.event.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.model.dto.CategoryDto;
import ru.practicum.category.model.mapper.CategoryMapper;
import ru.practicum.category.service.CategoryService;
import ru.practicum.event.model.*;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.StatisticsPostResponseDto;
import ru.practicum.event.model.dto.*;
import ru.practicum.event.model.mapper.EventMapper;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.BadParameterException;
import ru.practicum.exception.CreateConditionException;
import ru.practicum.exception.DataConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.model.dto.RequestDto;
import ru.practicum.client.RequestClient;
import ru.practicum.user.model.User;
import ru.practicum.user.model.dto.UserRequest;
import ru.practicum.user.model.mapper.UserMapper;
import ru.practicum.client.UserClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import ru.practicum.exception.ConflictException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.HOURS;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final EventRepository eventJpaRepository;
    private final CategoryService categoryService;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final EntityManager entityManager;
    private final StatsClient statsClient;


    @Transactional
    @Override
    public EventFullDto create(NewEventDto newEventDto, int userId) {
        LocalDateTime newEventDateTime = LocalDateTime.parse(newEventDto.getEventDate(), TIME_FORMAT);
        if (HOURS.between(LocalDateTime.now(), newEventDateTime) < 2) {
            throw new ValidationException("Начало события должно быть минимум на два часа позднее текущего момента");
        }

        Category category = CategoryMapper.toCategory(categoryService.getById(newEventDto.getCategory()));
        User user = getUserReference(userId);

        if (newEventDto.getDescription().trim().isEmpty() || newEventDto.getAnnotation().trim().isEmpty() || newEventDto.getParticipantLimit() < 0) {
            throw new ValidationException("Описание пустое");
        }
        Event event = EventMapper.toEvent(newEventDto, category, user);
        Event savedEvent = eventJpaRepository.save(event);

        return EventMapper.toFullDto(savedEvent, 0);
    }

    private User getUserReference(long userId) {
        User user = ensureUserExists(userId);
        return entityManager.contains(user) ? user : entityManager.getReference(User.class, userId);
    }

    private void validateUserExists(long userId) {
        UserRequest userRequest = fetchUserFromService(userId);
        if (userRequest != null) {
            return;
        }
        User existing = entityManager.find(User.class, userId);
        if (existing == null) {
            throw new NotFoundException(String.format("Пользователь с id=%d не найден", userId));
        }
    }

    private User ensureUserExists(long userId) {
        UserRequest userRequest = fetchUserFromService(userId);
        if (userRequest != null) {
            return upsertLocalUser(userRequest);
        }
        User existing = entityManager.find(User.class, userId);
        if (existing == null) {
            throw new NotFoundException(String.format("Пользователь с id=%d не найден", userId));
        }
        return existing;
    }

    private UserRequest fetchUserFromService(long userId) {
        try {
            return userClient.getById(userId);
        } catch (RuntimeException ex) {
            return null;
        }
    }


    private User upsertLocalUser(UserRequest userRequest) {
        User existing = entityManager.find(User.class, userRequest.getId());
        if (existing != null) {
            existing.setName(userRequest.getName());
            existing.setEmail(userRequest.getEmail());
            return entityManager.merge(existing);
        }
        User user = UserMapper.toUser(userRequest);
        return entityManager.merge(user);
    }
    private User findUserByEmail(String email) {
        TypedQuery<User> query = entityManager.createQuery(
                "select u from User u where u.email = :email", User.class);
        query.setParameter("email", email);
        return query.getResultStream().findFirst().orElse(null);
    }
    public List<EventShortDto> getEventsByCategory(int catId) {
        if (catId <= 0) {
            throw new BadParameterException("Id категории должен быть >0");
        }
        List<Event> events = eventJpaRepository.findByCategoryId(catId);
        if (events.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Long> idViewsMap = statsClient.getMapIdViews(events.stream()
                .map(Event::getId)
                .collect(Collectors.toList()));

        return events.stream()
                .map(e -> EventMapper.toShortDto(e, idViewsMap.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }


    public List<EventShortDto> getAllByUser(int userId, int from, int size) {
        validateUserExists(userId);
        PageRequest page = PageRequest.of(from / size, size, Sort.by("id").ascending());

        List<Event> events = eventJpaRepository.getAllByUser(userId, page);
        Map<Long, Long> idViewsMap = statsClient.getMapIdViews(events.stream()
                .map(Event::getId)
                .collect(Collectors.toList()));

        return events.stream()
                .map(e -> EventMapper.toShortDto(e, idViewsMap.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }

    public EventFullDto getByUserAndId(int userId, int eventId) {
        validateUserExists(userId);
        Event event = eventJpaRepository.getByIdAndUserId(eventId, userId);
        if (event == null) {
            throw new NotFoundException(String.format("События с id=%d и initiatorId=%d не найдено", eventId, userId));
        }
        Map<Long, Long> idViewsMap = statsClient.getMapIdViews(List.of(event.getId()));

        return EventMapper.toFullDto(event, idViewsMap.getOrDefault(event.getId(), 0L));
    }


    public EventFullDto getEvent(long eventId) {
        Event event = eventJpaRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("События с id=%d не найдено", eventId)));

        Map<Long, Long> idViewsMap = statsClient.getMapIdViews(List.of(event.getId()));
        return EventMapper.toFullDto(event, idViewsMap.getOrDefault(event.getId(), 0L));
    }

    public EventFullDto getEvent(int eventId, HttpServletRequest request) {
        EventFullDto eventDto = this.getEvent(eventId);
        if (eventDto.getState() != EventState.PUBLISHED) {
            throw new NotFoundException(String.format("Событие с id=%d не опубликовано", eventId));
        }
        StatisticsPostResponseDto endpointHitDto = new StatisticsPostResponseDto();
        endpointHitDto.setApp("ewm-main-event-service");
        endpointHitDto.setIp(request.getRemoteAddr());
        endpointHitDto.setTimestamp(LocalDateTime.now().format(TIME_FORMAT));
        endpointHitDto.setUri(request.getRequestURI());

        statsClient.postHit(endpointHitDto);

        return eventDto;
    }


    @Transactional
    public EventFullDto updateEvent(int userId, int eventId, UpdateEventUserRequest updateRequest) {
        Event event = eventJpaRepository.getByIdAndUserId(eventId, userId);
        if (event == null) {
            throw new NotFoundException(String.format("События с id=%d и initiatorId=%d не найдено", eventId, userId));
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new BadParameterException("Нельзя обновлять событие в состоянии 'Опубликовано'");
        }

        String annotation = updateRequest.getAnnotation();
        if (!(annotation == null || annotation.isBlank())) {
            event.setAnnotation(annotation);
        }
        Long categoryId = updateRequest.getCategory();
        if (categoryId != null && categoryId > 0) {
            CategoryDto categoryDto = categoryService.getById(categoryId);
            if (categoryDto != null) {
                event.setCategory(CategoryMapper.toCategory(categoryDto));
            }
        }
        String newDateString = updateRequest.getEventDate();
        if (!(newDateString == null || newDateString.isBlank())) {
            LocalDateTime newDate = LocalDateTime.parse(newDateString, TIME_FORMAT);
            if (HOURS.between(LocalDateTime.now(), newDate) < 2) {
                throw new ValidationException("Начало события должно быть минимум на два часа позднее текущего момента");
            }
            event.setEventDate(newDate);
        }
        Location location = updateRequest.getLocation();
        if (location != null) {
            event.setLocation(location);
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            if (updateRequest.getParticipantLimit() < 0) {
                throw new ValidationException("Participant limit cannot be negative");
            }
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }


        String stateString = updateRequest.getStateAction();
        if (stateString != null && !stateString.isBlank()) {
            switch (StateActionUser.valueOf(stateString)) {
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
            }
        }
        String title = updateRequest.getTitle();
        if (!(title == null || title.isBlank())) {
            event.setTitle(title);
        }

        eventJpaRepository.save(event);
        Map<Long, Long> idViewsMap = statsClient.getMapIdViews(List.of(event.getId()));

        Event updatedEvent = eventJpaRepository.findById(event.getId())
                .orElseThrow(() -> new NotFoundException(String.format("Событие с id=%d не найден", event.getId())));

        return EventMapper.toFullDto(updatedEvent, idViewsMap.getOrDefault(event.getId(), 0L));
    }


    @Transactional
    public EventFullDto updateAdminEvent(long eventId, UpdateEventAdminRequest adminRequest) {
        Event event = eventJpaRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("События с id=%d не найдено", eventId)));

        String annotation = adminRequest.getAnnotation();
        if (!(annotation == null || annotation.isBlank())) {
            event.setAnnotation(annotation);
        }
        Long categoryId = adminRequest.getCategory();
        if (categoryId != null && categoryId > 0) {
            CategoryDto categoryDto = categoryService.getById(categoryId);
            if (categoryDto != null) {
                event.setCategory(CategoryMapper.toCategory(categoryDto));
            }
        }
        String description = adminRequest.getDescription();
        if (!(description == null || description.isBlank())) {
            event.setDescription(description);
        }

        String newDateString = adminRequest.getEventDate();
        if (!(newDateString == null || newDateString.isBlank())) {
            LocalDateTime newDate = LocalDateTime.parse(newDateString, TIME_FORMAT);
            if (HOURS.between(LocalDateTime.now(), newDate) < 2) {
                throw new ValidationException("Начало события должно быть минимум на два часа позднее текущего момента");
            }
            event.setEventDate(newDate);
        }
        Location location = adminRequest.getLocation();
        if (location != null) {
            event.setLocation(location);
        }
        if (adminRequest.getPaid() != null) {
            event.setPaid(adminRequest.getPaid());
        }
        if (adminRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(adminRequest.getParticipantLimit());
        }
        if (adminRequest.getRequestModeration() != null) {
            event.setRequestModeration(adminRequest.getRequestModeration());
        }


        String stateString = adminRequest.getStateAction();
        if (stateString != null && !stateString.isBlank()) {
            switch (StateActionAdmin.valueOf(stateString)) {
                case PUBLISH_EVENT:
                    if (HOURS.between(LocalDateTime.now(), event.getEventDate()) < 1) {
                        throw new CreateConditionException("Начало события должно быть минимум на один час позже момента публикации");
                    }
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new DataConflictException(String.format("Попытка опубликовать событие с id=%d, которое уже опубликовано.", event.getId()));
                    }
                    if (event.getState() == EventState.CANCELED) {
                        throw new DataConflictException(String.format("Попытка опубликовать событие с id=%d, которое уже отменено.", event.getId()));
                    }
                    event.setState(EventState.PUBLISHED);
                    break;
                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new DataConflictException(String.format("Попытка отменить событие с id=%d, которое уже опубликовано.", event.getId()));
                    }
                    event.setState(EventState.CANCELED);
                    break;
            }
        }
        String title = adminRequest.getTitle();
        if (!(title == null || title.isBlank())) {
            event.setTitle(title);
        }

        eventJpaRepository.save(event);
        Map<Long, Long> idViewsMap = statsClient.getMapIdViews(List.of(event.getId()));

        Event updatedEvent = eventJpaRepository.findById(event.getId())
                .orElseThrow(() -> new NotFoundException(String.format("Событие с id=%d не найден", event.getId())));

        return EventMapper.toFullDto(updatedEvent, idViewsMap.getOrDefault(event.getId(), 0L));
    }

    @Override
    public Set<Event> getEventsByIds(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }
        //события выгружаются сразу с инициаторами по EAGER
        List<Event> eventList = eventJpaRepository.findEventsWIthUsersByIdSet(eventIds); //получение списка событий из репозитория
        return new HashSet<>(eventList);
    }

    @Override
    @Transactional
    public List<RequestDto> getParticipationInfo(Long userId, Long eventId) {

        Event event = eventJpaRepository.getByIdAndUserId(eventId, userId);
        if (event == null) {
            throw new NotFoundException(String.format("События с id=%d и initiatorId=%d не найдено", eventId, userId));
        }
        return getEventRequests(event.getId());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest) {
        Event event = eventJpaRepository.getByIdAndUserId(eventId, userId);
        if (event == null) {
            throw new NotFoundException(String.format("События с id=%d и initiatorId=%d не найдено", eventId, userId));
        }

        List<RequestDto> requests = getEventRequests(eventId);
        int limit = event.getParticipantLimit();

        if (updateRequest.getStatus() == UpdateRequestState.REJECTED) {
            return rejectRequests(event, requests, updateRequest);
        } else {
            if ((limit == 0 || !event.isRequestModeration())) {
                return confirmAllRequests(event, requests, updateRequest);
            } else {
                return confirmRequests(event, requests, updateRequest);
            }
        }
    }

    @Transactional
    public List<EventFullDto> getEventsAdmin(EventParam p) {
        List<Long> users = p.getUsers();
        List<String> states = p.getStates();
        List<Long> categories = p.getCategories();
        LocalDateTime rangeStart = p.getRangeStart();
        LocalDateTime rangeEnd = p.getRangeEnd();
        int from = p.getFrom();
        int size = p.getSize();

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> criteriaQuery = criteriaBuilder.createQuery(Event.class);
        Root<Event> eventRoot = criteriaQuery.from(Event.class);
        criteriaQuery = criteriaQuery.select(eventRoot);

        List<Event> resultEvents;
        Predicate complexPredicate = null;
        if (rangeStart != null && rangeEnd != null) {
            complexPredicate
                    = criteriaBuilder.between(eventRoot.get("eventDate").as(LocalDateTime.class), rangeStart, rangeEnd);
        }
        if (users != null && !users.isEmpty()) {
            Predicate predicateForUsersId
                    = eventRoot.get("initiator").get("id").in(users);
            if (complexPredicate == null) {
                complexPredicate = predicateForUsersId;
            } else {
                complexPredicate = criteriaBuilder.and(complexPredicate, predicateForUsersId);
            }
        }
        if (categories != null && !categories.isEmpty()) {
            Predicate predicateForCategoryId
                    = eventRoot.get("category").get("id").in(categories);
            if (complexPredicate == null) {
                complexPredicate = predicateForCategoryId;
            } else {
                complexPredicate = criteriaBuilder.and(complexPredicate, predicateForCategoryId);
            }
        }
        if (states != null && !states.isEmpty()) {
            Predicate predicateForStates
                    = eventRoot.get("state").as(String.class).in(states);
            if (complexPredicate == null) {
                complexPredicate = predicateForStates;
            } else {
                complexPredicate = criteriaBuilder.and(complexPredicate, predicateForStates);
            }
        }
        if (complexPredicate != null) {
            criteriaQuery.where(complexPredicate);
        }
        TypedQuery<Event> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(from);
        typedQuery.setMaxResults(size);
        resultEvents = typedQuery.getResultList();

        Map<Long, Long> idViewsMap = statsClient.getMapIdViews(resultEvents.stream().map(Event::getId).collect(Collectors.toList()));

        return resultEvents.stream()
                .map(e -> EventMapper.toFullDto(e, idViewsMap.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }


    @Transactional
    public List<EventShortDto> getEvents(EventParam p) {
        String text = p.getText();
        List<Long> categories = p.getCategories();
        LocalDateTime rangeStart = p.getRangeStart();
        LocalDateTime rangeEnd = p.getRangeEnd();
        Boolean paid = p.getPaid();
        Boolean onlyAvailable = p.getOnlyAvailable();
        int from = p.getFrom();
        int size = p.getSize();
        String sort = p.getSort();
        HttpServletRequest request = p.getRequest();

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> criteriaQuery = criteriaBuilder.createQuery(Event.class);
        Root<Event> eventRoot = criteriaQuery.from(Event.class);
        criteriaQuery.select(eventRoot);

        LocalDateTime effectiveRangeStart = rangeStart != null ? rangeStart : LocalDateTime.now();
        LocalDateTime effectiveRangeEnd = rangeEnd != null ? rangeEnd : LocalDateTime.of(9999, 1, 1, 1, 1, 1);
        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new ValidationException("Начало диапазона не может быть позже конца диапазона");
        }
        Predicate complexPredicate = criteriaBuilder.between(
                eventRoot.get("eventDate").as(LocalDateTime.class),
                effectiveRangeStart,
                effectiveRangeEnd
        );

        if (text != null && !text.isBlank()) {
            String decodeText = URLDecoder.decode(text, StandardCharsets.UTF_8);

            Expression<String> annotationLowerCase = criteriaBuilder.lower(eventRoot.get("annotation"));
            Expression<String> descriptionLowerCase = criteriaBuilder.lower(eventRoot.get("description"));
            Predicate predicateForAnnotation = criteriaBuilder.like(annotationLowerCase, "%" + decodeText.toLowerCase() + "%");
            Predicate predicateForDescription = criteriaBuilder.like(descriptionLowerCase, "%" + decodeText.toLowerCase() + "%");
            Predicate predicateForText = criteriaBuilder.or(predicateForAnnotation, predicateForDescription);
            complexPredicate = criteriaBuilder.and(complexPredicate, predicateForText);
        }

        if (categories != null && !categories.isEmpty()) {
            if (categories.stream().anyMatch(c -> c <= 0)) {
                throw new ValidationException("Id категории должен быть > 0");
            }
            Predicate predicateForCategoryId = eventRoot.get("category").get("id").in(categories);
            complexPredicate = criteriaBuilder.and(complexPredicate, predicateForCategoryId);
        }

        if (paid != null) {
            Predicate predicateForPaid = criteriaBuilder.equal(eventRoot.get("paid"), paid);
            complexPredicate = criteriaBuilder.and(complexPredicate, predicateForPaid);
        }

        if (onlyAvailable != null && onlyAvailable) {
            Predicate noLimit = criteriaBuilder.equal(eventRoot.get("participantLimit"), 0);
            Predicate hasSlots = criteriaBuilder.lt(eventRoot.get("confirmedRequests"), eventRoot.get("participantLimit"));
            Predicate predicateForOnlyAvailable = criteriaBuilder.or(noLimit, hasSlots);
            complexPredicate = criteriaBuilder.and(complexPredicate, predicateForOnlyAvailable);
        }

        Predicate predicateForPublished = criteriaBuilder.equal(eventRoot.get("state"), EventState.PUBLISHED);
        complexPredicate = criteriaBuilder.and(complexPredicate, predicateForPublished);

        criteriaQuery.where(complexPredicate);

        TypedQuery<Event> typedQuery = entityManager.createQuery(criteriaQuery);

        List<Event> resultEvents = typedQuery.getResultList();

        StatisticsPostResponseDto endpointHitDto = new StatisticsPostResponseDto();
        endpointHitDto.setApp("ewm-main-event-service");
        endpointHitDto.setIp(request.getRemoteAddr());
        endpointHitDto.setTimestamp(LocalDateTime.now().format(TIME_FORMAT));
        endpointHitDto.setUri(request.getRequestURI());

        statsClient.postHit(endpointHitDto);

        Map<Long, Long> idViews = statsClient.getMapIdViews(resultEvents.stream().map(Event::getId).collect(Collectors.toList()));

        Comparator<EventShortDto> comparator;
        if (sort != null && sort.equals("EVENT_DATE")) {
            comparator = Comparator.comparing(e -> LocalDateTime.parse(e.getEventDate(), TIME_FORMAT));
        } else {
            comparator = Comparator.comparing(EventShortDto::getViews);
        }

        List<EventShortDto> sortedEvents = resultEvents.stream()
                .map(e -> EventMapper.toShortDto(e, idViews.getOrDefault(e.getId(), 0L)))
                .sorted(comparator)
                .collect(Collectors.toList());
        int startIndex = Math.min(from, sortedEvents.size());
        int endIndex = Math.min(from + size, sortedEvents.size());
        return sortedEvents.subList(startIndex, endIndex);
    }

    public Set<EventFullDto> getEventsByIdSet(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Event> eventList = eventJpaRepository.findByIdIn(eventIds);

        if (eventList == null || eventList.isEmpty()) {
            return new HashSet<>();
        }
        Map<Long, Long> idViews = statsClient.getMapIdViews(eventList.stream().map(Event::getId).collect(Collectors.toList()));

        return eventList.stream()
                .map(e -> EventMapper.toFullDto(e, idViews.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toSet());
    }

    @Transactional
    protected EventRequestStatusUpdateResult rejectRequests(Event event, List<RequestDto> requests, EventRequestStatusUpdateRequest updateRequest) {
        EventRequestStatusUpdateResult updateResult = new EventRequestStatusUpdateResult();
        Map<Long, RequestDto> prDtoMap = requests.stream()
                .collect(Collectors.toMap(RequestDto::getId, e -> e));
        for (long id : updateRequest.getRequestIds()) {
            RequestDto prDto = prDtoMap.get(id);
            if (prDto == null) {
                throw new NotFoundException(String.format("Запросу на обновление статуса, не найдено событие с id=%d", id));
            }
            if (prDto.getStatus().equals(RequestStatus.PENDING)) {
                prDto.setStatus(RequestStatus.REJECTED);
                updateResult.getRejectedRequests().add(prDto);
            } else {
                throw new CreateConditionException(String.format("Нельзя отклонить уже обработанную заявку id=%d", id));
            }
        }
        updateEventRequests(event.getId(), updateResult.getRejectedRequests());
        return updateResult;
    }


    @Transactional
    protected EventRequestStatusUpdateResult confirmAllRequests(Event event, List<RequestDto> requests, EventRequestStatusUpdateRequest updateRequest) {
        int confirmedRequestsAmount = event.getConfirmedRequests();
        int limit = event.getParticipantLimit();
        if (limit > 0 && confirmedRequestsAmount >= limit) {
            throw new CreateConditionException("Лимит участников достигнут");
        }
        EventRequestStatusUpdateResult updateResult = new EventRequestStatusUpdateResult();
        Map<Long, RequestDto> prDtoMap = requests.stream()
                .collect(Collectors.toMap(RequestDto::getId, e -> e));
        for (long id : updateRequest.getRequestIds()) {
            RequestDto prDto = prDtoMap.get(id);
            if (prDto == null) {
                throw new NotFoundException(String.format("Запросу на обновление статуса, не найдено событие с id=%d", id));
            }
            if (prDto.getStatus().equals(RequestStatus.PENDING)) {
                if (limit > 0 && confirmedRequestsAmount >= limit) {
                    prDto.setStatus(RequestStatus.REJECTED);
                    updateResult.getRejectedRequests().add(prDto);
                    continue;
                }
                prDto.setStatus(RequestStatus.CONFIRMED);
                confirmedRequestsAmount++;
                event.setConfirmedRequests(confirmedRequestsAmount);
                updateResult.getConfirmedRequests().add(prDto);
            } else {
                throw new CreateConditionException(String.format("Нельзя подтвердить уже обработанную заявку id=%d", id));
            }
        }
        eventJpaRepository.save(event);
        updateEventRequests(event.getId(), updateResult.getConfirmedRequests());
        updateEventRequests(event.getId(), updateResult.getRejectedRequests());
        return updateResult;
    }


    @Transactional
    protected EventRequestStatusUpdateResult confirmRequests(Event event, List<RequestDto> requests, EventRequestStatusUpdateRequest updateRequest) {
        int confirmedRequestsAmount = event.getConfirmedRequests();
        int limit = event.getParticipantLimit();
        if (confirmedRequestsAmount >= limit) {
            throw new CreateConditionException("Лимит участников достигнут");
        }
        EventRequestStatusUpdateResult updateResult = new EventRequestStatusUpdateResult();
        Map<Long, RequestDto> prDtoMap = requests.stream()
                .collect(Collectors.toMap(RequestDto::getId, e -> e));
        for (long id : updateRequest.getRequestIds()) {
            RequestDto prDto = prDtoMap.get(id);
            if (prDto == null) {
                throw new NotFoundException(String.format("Запросу на обновление статуса, не найдено событие с id=%d", id));
            }
            if (prDto.getStatus().equals(RequestStatus.PENDING)) {
                if (confirmedRequestsAmount >= limit) {
                    prDto.setStatus(RequestStatus.REJECTED);
                    updateResult.getRejectedRequests().add(prDto);
                    continue;
                }
                prDto.setStatus(RequestStatus.CONFIRMED);
                confirmedRequestsAmount++;
                event.setConfirmedRequests(confirmedRequestsAmount);
                updateResult.getConfirmedRequests().add(prDto);
            } else {
                throw new CreateConditionException(String.format("Нельзя подтвердить уже обработанную заявку id=%d", id));
            }
        }
        eventJpaRepository.save(event);
        updateEventRequests(event.getId(), updateResult.getConfirmedRequests());
        updateEventRequests(event.getId(), updateResult.getRejectedRequests());
        return updateResult;
    }

    @Retry(name = "request-service", fallbackMethod = "getEventRequestsFallback")
    @CircuitBreaker(name = "request-service", fallbackMethod = "getEventRequestsFallback")
    private List<RequestDto> getEventRequests(Long eventId) {
        return requestClient.getEventRequests(eventId);
    }

    @SuppressWarnings("unused")
    private List<RequestDto> getEventRequestsFallback(Long eventId, Throwable ex) {
        return new ArrayList<>();
    }

    @Retry(name = "request-service", fallbackMethod = "updateEventRequestsFallback")
    @CircuitBreaker(name = "request-service", fallbackMethod = "updateEventRequestsFallback")
    private void updateEventRequests(Long eventId, List<RequestDto> requests) {
        requestClient.updateEventRequests(eventId, requests);
    }

    @SuppressWarnings("unused")
    private void updateEventRequestsFallback(Long eventId, List<RequestDto> requests, Throwable ex) {
    }

}