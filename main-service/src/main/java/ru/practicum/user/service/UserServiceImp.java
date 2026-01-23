package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.PaginationException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.model.User;
import ru.practicum.user.model.dto.UserDto;
import ru.practicum.user.model.dto.UserRequest;
import ru.practicum.user.model.mapper.UserMapper;
import ru.practicum.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImp implements UserService {
    private final UserRepository userRepository;

    @Override
    public UserDto addUser(UserRequest userRequest) {

        User currentUser = userRepository.getByEmail(userRequest.getEmail());
        if (currentUser != null) {
            throw new ConflictException("User already exists");
        }
        User newUser = userRepository.save(UserMapper.toUser(userRequest));
        return UserMapper.toUserDto(newUser);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
        if (from != null && from < 0) {
            throw new PaginationException("The 'from' parameter must be >= 0");
        }
        if (size != null && size < 1) {
            throw new PaginationException("The 'size' parameter must be >= 1");
        }

        List<User> users;

        if (ids != null && !ids.isEmpty()) {
            users = userRepository.findAllByIdInOrderById(ids);
        } else {
            PageRequest page = PageRequest.of(
                    from != null ? from : 0,
                    size != null ? size : Integer.MAX_VALUE,
                    Sort.by("id").ascending()
            );
            users = userRepository.findAll(page).getContent();
        }

        return users.stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d not found", id)));
        userRepository.deleteById(id);
    }

    @Override
    public UserDto getUserById(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id=%d не найден", userId)));

        return UserMapper.toUserDto(user);
    }

    @Override
    public List<UserDto> getAllUsers(List<Long> ids) {
        List<User> users; //список пользователей
        if (ids == null || ids.isEmpty()) { //если список id пустой, то Возвращаем пустой списко
            return new ArrayList<>();
        } else {
            users = userRepository.findAllByIdIn(ids);
        }
        return users.stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }
}
