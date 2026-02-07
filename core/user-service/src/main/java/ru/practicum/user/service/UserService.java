package ru.practicum.user.service;

import ru.practicum.user.model.dto.UserRequest;

import java.util.List;

public interface UserService {
    UserRequest addUser(UserRequest userRequest);

    List<UserRequest> getUsers(List<Long> ids, Integer from, Integer size);

    void deleteUser(Long id);

    UserRequest getUserById(long userId);

    List<UserRequest> getAllUsers(List<Long> ids);

    boolean existsById(Long userId);
}
