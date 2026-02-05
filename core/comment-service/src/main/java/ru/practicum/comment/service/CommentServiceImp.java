package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.dto.CommentRequest;
import ru.practicum.comment.model.dto.CommentResponse;
import ru.practicum.comment.model.mapper.CommentMapper;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImp implements CommentService {
    private final UserClient userClient;
    private final EventClient eventClient;
    private final CommentRepository commentRepository;


    @Override
    @Transactional
    public CommentResponse createComment(Long userId, Long eventId, CommentRequest commentRequest) {
        if (!isUserExists(userId)) {
            throw new NotFoundException(String.format("Пользователь с id = %d не найден", userId));
        }
        if (!isEventExists(eventId)) {
            throw new NotFoundException(String.format("Событие с id = %d не найдено", eventId));
        }

        Comment comment = new Comment();
        comment.setText(commentRequest.getText());
        comment.setCreated(LocalDateTime.now());
        comment.setAuthorId(userId);
        comment.setEventId(eventId);

        Comment newComment = commentRepository.save(comment);
        return CommentMapper.toCommentResponse(newComment);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, CommentRequest commentRequest) {
        Comment comment = commentRepository.findById(commentId).get();
        if (comment == null) {
            throw new NotFoundException(String.format("Комментарий с id = %d не найден", commentId));
        }

        if (!isUserExists(userId)) {
            throw new NotFoundException(String.format("Пользователь с id = %d не найден", userId));
        }

        if (!comment.getAuthorId().equals(userId)) {
            throw new ConflictException("Только автор может редактировать комментарий");
        }
        comment.setText(commentRequest.getText());
        Comment updatedComment = commentRepository.save(comment);
        return CommentMapper.toCommentResponse(updatedComment);
    }

    @Override
    public List<CommentResponse> getCommentsByEvent(Long eventId) {
        if (!isEventExists(eventId)) {
            throw new NotFoundException(String.format("Событие с id = %d не найдено", eventId));
        }
        return commentRepository.findAllByEventIdOrderByCreatedAsc(eventId).stream()
                .map(CommentMapper::toCommentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CommentResponse getCommentById(Long eventId, Long commentId) {
        // также валидируем существование события (опционально, но полезно для консистентности URL)
        if (!isEventExists(eventId)) {
            throw new NotFoundException(String.format("Событие с id = %d не найдено", eventId));
        }
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий с id = %d не найден", commentId)));

        if (!comment.getEventId().equals(eventId)) {
            throw new NotFoundException(String.format("Комментарий с id = %d не принадлежит указанному событию  с id = %d", commentId, eventId));
        }

        return CommentMapper.toCommentResponse(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий с id = %d не найден", commentId)));

        // Проверка автора
        if (!comment.getAuthorId().equals(userId)) {
            throw new ConflictException("Только автор может удалять комментарий");
        }

        commentRepository.deleteById(commentId);
    }


    @Retry(name = "user-service", fallbackMethod = "isUserExistsFallback")
    @CircuitBreaker(name = "user-service", fallbackMethod = "isUserExistsFallback")
    private boolean isUserExists(Long userId) {
        return userClient.existsById(userId);
    }

    @SuppressWarnings("unused")
    private boolean isUserExistsFallback(Long userId, Throwable ex) {
        return false;
    }

    @Retry(name = "event-service", fallbackMethod = "isEventExistsFallback")
    @CircuitBreaker(name = "event-service", fallbackMethod = "isEventExistsFallback")
    private boolean isEventExists(Long eventId) {
        return eventClient.existsById(eventId);
    }

    @SuppressWarnings("unused")
    private boolean isEventExistsFallback(Long eventId, Throwable ex) {
        return false;
    }
}
