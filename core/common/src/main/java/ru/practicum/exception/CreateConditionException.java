package ru.practicum.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CreateConditionException extends RuntimeException {
    public CreateConditionException(String mess) {
        super(mess);
    }
}
