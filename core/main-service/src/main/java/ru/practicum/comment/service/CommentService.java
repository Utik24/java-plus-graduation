package ru.practicum.comment.service;

import ru.practicum.comment.model.dto.CommentRequest;
import ru.practicum.comment.model.dto.CommentResponse;

import java.util.List;

public interface CommentService {
    CommentResponse createComment(Long userId, Long eventId, CommentRequest commentRequest);

    CommentResponse updateComment(Long userId, Long eventId, CommentRequest commentRequest);

    List<CommentResponse> getCommentsByEvent(Long eventId);

    CommentResponse getCommentById(Long eventId, Long commentId);

    void deleteComment(Long userId, Long commentId);
}
