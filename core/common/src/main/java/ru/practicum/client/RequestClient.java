package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.model.dto.RequestDto;

import java.util.List;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/internal/events/{eventId}/requests")
    List<RequestDto> getEventRequests(@PathVariable("eventId") Long eventId);

    @PostMapping("/internal/events/{eventId}/requests/status")
    void updateEventRequests(@PathVariable("eventId") Long eventId,
                             @RequestBody List<RequestDto> requests);
}