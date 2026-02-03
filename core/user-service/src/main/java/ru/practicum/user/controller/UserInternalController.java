package ru.practicum.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.user.model.dto.UserDto;
import ru.practicum.user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserInternalController {

    private final UserService userService;

    @GetMapping("/{userId}/exists")
    public boolean exists(@PathVariable("userId") Long userId) {
        return userService.existsById(userId);
    }

    @GetMapping("/{userId}")
    public UserDto getById(@PathVariable("userId") Long userId) {
        return userService.getUserById(userId);
    }

    @GetMapping
    public List<UserDto> getByIds(@RequestParam("ids") List<Long> ids) {
        return userService.getAllUsers(ids);
    }
}