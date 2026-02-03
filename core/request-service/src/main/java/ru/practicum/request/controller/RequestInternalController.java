package ru.practicum.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.model.dto.RequestDto;
import ru.practicum.request.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/internal/events/{eventId}/requests")
@RequiredArgsConstructor
public class RequestInternalController {

    private final RequestService requestService;

    @GetMapping
    public List<RequestDto> getEventRequests(@PathVariable("eventId") Long eventId) {
        return requestService.getAllRequestsEventId(eventId);
    }

    @PostMapping("/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateEventRequests(@PathVariable("eventId") Long eventId,
                                    @RequestBody List<RequestDto> requestDtos) {
        requestService.updateEventRequests(eventId, requestDtos);
    }
}