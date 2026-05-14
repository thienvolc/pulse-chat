package com.pulse.chat.app.aop;

import com.pulse.chat.app.dto.response.ErrorResponse;
import com.pulse.chat.app.dto.response.ErrorResponse.ValidationError;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

import static com.pulse.chat.domain.common.constant.ResponseCode.*;
import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class AppExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleException(BusinessException ex) {
        ErrorResponse response = ErrorResponse.fromCodeAndMessage(
                ex.getResponseCode().getCode(), ex.getMessage());

        log.info("Business Exception: {}", response);
        log.debug(ex.getMessage(), ex);

        return new ResponseEntity<>(response, ex.getResponseCode().getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleException(
            MethodArgumentNotValidException ex) {

        List<ValidationError> errors = getAllValidationErrors(ex);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.fromValidationErrors(errors));
    }

    private List<ValidationError> getAllValidationErrors(
            MethodArgumentNotValidException ex) {

        return ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ValidationError(e.getField(), e.getCode(), e.getDefaultMessage()))
                .toList();
    }

    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleException(BadCredentialsException ex) {
        log.error(ex.getMessage(), ex);
        return ResponseEntity.status(UNAUTHORIZED)
                .body(ErrorResponse.fromErrorCode(BAD_CREDENTIALS));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleException(AccessDeniedException ex) {
        log.error(ex.getMessage(), ex);
        return ResponseEntity.status(FORBIDDEN)
                .body(ErrorResponse.fromErrorCode(ACCESS_DENIED));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleException(EntityNotFoundException ex) {
        log.error(ex.getMessage(), ex);
        return ResponseEntity.status(NOT_FOUND)
                .body(ErrorResponse.fromCodeAndMessage("TBD", ex.getMessage()));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleException(UsernameNotFoundException ex) {
        log.error(ex.getMessage(), ex);
        return ResponseEntity.status(NOT_FOUND)
                .body(ErrorResponse.fromErrorCode(USERNAME_NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error(ex.getMessage(), ex);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.fromErrorCode(INTERNAL_EXCEPTION));
    }
}
