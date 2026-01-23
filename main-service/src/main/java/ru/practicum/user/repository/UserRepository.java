package ru.practicum.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.user.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    User getByEmail(String email);

    List<User> findAllByIdInOrderById(List<Long> ids);

    List<User> findAllByIdIn(List<Long> ids);

    Optional<User> findById(long id);
}
