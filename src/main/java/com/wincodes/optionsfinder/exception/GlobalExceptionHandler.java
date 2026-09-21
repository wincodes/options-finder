package com.wincodes.optionsfinder.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(Exception exception) {

        return new ErrorResponse(
                "INTERNAL_ERROR",
                exception.getMessage(),
                OffsetDateTime.now()
        );
    }

    public record ErrorResponse(
            String code,
            String message,
            OffsetDateTime timestamp
    ) {}
}
