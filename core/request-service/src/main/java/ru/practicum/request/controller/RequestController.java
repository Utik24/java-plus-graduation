package ru.practicum.request.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.model.dto.RequestDto;
import ru.practicum.request.service.RequestService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/users/{userId}/requests")
@AllArgsConstructor
public class RequestController {

    private final RequestService service;

    @GetMapping
    public List<RequestDto> getAll(@PathVariable("userId") Long userId) {
        return service.getAll(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequestDto create(@PathVariable("userId") Long userId,
                             @RequestParam(name = "eventId", required = false) Long eventId) {
        return service.create(userId, eventId);
    }

    @PatchMapping("/{requestId}/cancel")
    public RequestDto cancelRequest(@PathVariable("userId") Long userId, @PathVariable("requestId") Long requestId) {
        return service.cancelRequest(userId, requestId);
    }

}
