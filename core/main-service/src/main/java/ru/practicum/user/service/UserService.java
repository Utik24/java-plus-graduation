package ru.practicum.user.service;

import ru.practicum.user.model.dto.UserDto;
import ru.practicum.user.model.dto.UserRequest;

import java.util.List;

public interface UserService {
    UserDto addUser(UserRequest userRequest);

    List<UserDto> getUsers(List<Long> ids, Integer from, Integer size);

    void deleteUser(Long id);

    UserDto getUserById(long userId);

    List<UserDto> getAllUsers(List<Long> ids);
}
