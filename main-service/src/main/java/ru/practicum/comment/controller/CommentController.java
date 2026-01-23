package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.model.dto.CommentRequest;
import ru.practicum.comment.model.dto.CommentResponse;
import ru.practicum.comment.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/event/{eventId}/comment")
@AllArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private static final String EVENT_ID = "eventId";
    private static final String COMMENT_ID = "commentId";
    private static final String X_USER_ID = "X-User-Id";
    private static final String COMMENT_ID_PATH = "/{commentId}";


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(@PathVariable(EVENT_ID) Long eventId,
                                         @RequestHeader(X_USER_ID) Long userId,
                                         @Valid @RequestBody CommentRequest commentRequest) {
        return commentService.createComment(userId, eventId, commentRequest);
    }

    @PatchMapping(COMMENT_ID_PATH)
    public CommentResponse updateComment(@PathVariable(EVENT_ID) Long eventId,
                                         @PathVariable(COMMENT_ID) Long commentId,
                                         @RequestHeader(X_USER_ID) Long userId,
                                         @Valid @RequestBody CommentRequest commentRequest) {
        return commentService.updateComment(userId, commentId, commentRequest);
    }

    @GetMapping
    public List<CommentResponse> getComments(@PathVariable(EVENT_ID) Long eventId) {
        return commentService.getCommentsByEvent(eventId);
    }

    @GetMapping(COMMENT_ID_PATH)
    public CommentResponse getComment(@PathVariable(EVENT_ID) Long eventId,
                                      @PathVariable(COMMENT_ID) Long commentId) {
        return commentService.getCommentById(eventId, commentId);
    }

    @DeleteMapping(COMMENT_ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable(EVENT_ID) Long eventId,
                              @PathVariable(COMMENT_ID) Long commentId,
                              @RequestHeader(X_USER_ID) Long userId) {
        commentService.deleteComment(userId, commentId);
    }

}
