package com.exequt.common.handler;

import com.exequt.common.exception.BusinessException;
import com.exequt.common.exception.ConflictException;
import com.exequt.common.exception.NotFoundException;
import com.exequt.common.response.GeneralResponse;
import com.exequt.common.response.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GeneralResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) {
            message = "Validation failed";
        }
        return respond(HttpStatus.BAD_REQUEST, ResultCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<GeneralResponse> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) {
            message = "Validation failed";
        }
        return respond(HttpStatus.BAD_REQUEST, ResultCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GeneralResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) {
            message = "Validation failed";
        }
        return respond(HttpStatus.BAD_REQUEST, ResultCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<GeneralResponse> handleNotFound(NotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, ex.getResultCode(), ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<GeneralResponse> handleConflict(ConflictException ex) {
        return respond(HttpStatus.CONFLICT, ex.getResultCode(), ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GeneralResponse> handleBusiness(BusinessException ex) {
        HttpStatus status = httpStatusFor(ex.getResultCode());
        return respond(status, ex.getResultCode(), ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<GeneralResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic locking conflict: {}", ex.getMessage());
        return respond(HttpStatus.CONFLICT, ResultCode.BUSINESS_CONFLICT, "Concurrent update conflict");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GeneralResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return respond(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResultCode.INTERNAL_ERROR,
                "An unexpected error occurred");
    }

    private static ResponseEntity<GeneralResponse> respond(HttpStatus httpStatus, ResultCode resultCode, String description) {
        return ResponseEntity.status(httpStatus).body(GeneralResponse.failure(resultCode, description));
    }

    private static HttpStatus httpStatusFor(ResultCode resultCode) {
        return switch (resultCode) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case BUSINESS_CONFLICT, DUPLICATE_EVENT -> HttpStatus.CONFLICT;
            case BAD_REQUEST, VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            case SUCCESS -> HttpStatus.OK;
        };
    }
}
