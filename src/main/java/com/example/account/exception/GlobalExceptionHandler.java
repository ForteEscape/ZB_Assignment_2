package com.example.account.exception;

import com.example.account.dto.ErrorResponse;
import com.example.account.type.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountException.class)
    public ErrorResponse handleAccountException(AccountException e){
        log.error("{} is occurred", e.getErrorCode());

        return ErrorResponse.builder()
                .errorCode(e.getErrorCode())
                .errorMessage(e.getErrorMessage())
                .build();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ErrorResponse handleDataIntegrityViolationException(DataIntegrityViolationException e){
        log.error("DataIntegrityViolationException occurred");

        return ErrorResponse.builder()
                .errorCode(ErrorCode.INVALID_REQUEST)
                .errorMessage(ErrorCode.INVALID_REQUEST.getDescription())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleDataIntegrityViolationException(MethodArgumentNotValidException e){
        log.error("MethodArgumentNotValidException occurred");

        return ErrorResponse.builder()
                .errorCode(ErrorCode.INVALID_REQUEST)
                .errorMessage(ErrorCode.INVALID_REQUEST.getDescription())
                .build();
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleException(Exception e){
        log.error("Exception is occurred");

        return ErrorResponse.builder()
                .errorCode(ErrorCode.INVALID_REQUEST)
                .errorMessage(ErrorCode.INVALID_REQUEST.getDescription())
                .build();
    }
}
