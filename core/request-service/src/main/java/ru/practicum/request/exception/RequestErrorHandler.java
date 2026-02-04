package ru.practicum.request.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.exception.CreateConditionException;
import ru.practicum.exception.ErrorResponse;

@RestControllerAdvice(basePackages = "ru.practicum.request")
public class RequestErrorHandler {

    @ExceptionHandler(CreateConditionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleCreateConditionException(final RuntimeException e) {
        return new ErrorResponse(
                "CONFLICT",
                "For the requested operation the conditions are not met.",
                e.getMessage()
        );
    }
}