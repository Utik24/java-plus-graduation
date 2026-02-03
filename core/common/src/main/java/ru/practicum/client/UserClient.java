package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.user.model.dto.UserDto;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/internal/users/{userId}/exists")
    boolean existsById(@PathVariable("userId") Long userId);

    @GetMapping("/internal/users/{userId}")
    UserDto getById(@PathVariable("userId") Long userId);

    @GetMapping("/internal/users")
    List<UserDto> getByIds(@RequestParam("ids") List<Long> ids);
}