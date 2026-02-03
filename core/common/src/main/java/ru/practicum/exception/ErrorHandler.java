package ru.practicum.exception;

import jakarta.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler({PaginationException.class, NotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundException(final RuntimeException e) {
        return new ErrorResponse(
                "NOT_FOUND",
                "The required object was not found.",
                e.getMessage()
        );
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(ValidationException e) {
        return new ErrorResponse(
                "BAD_REQUEST",
                "Incorrectly made request.",
                e.getMessage()
        );
    }

    @ExceptionHandler({BadParameterException.class, CreateConditionException.class, DataConflictException.class, ConflictException.class, ConstraintException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConstraintException(final RuntimeException e) {
        return new ErrorResponse(
                "FORBIDDEN",
                "For the requested operation the conditions are not met.",
                e.getMessage()
        );
    }
}
