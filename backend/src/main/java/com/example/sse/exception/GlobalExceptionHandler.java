package com.example.sse.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 끊긴 SSE 연결에 쓰다 실패한 경우: 받을 클라이언트가 없으므로 응답을 쓰지 않는다
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void asyncRequestNotUsableExceptionHandle(AsyncRequestNotUsableException ex) {
        log.debug("Client disconnected - {}", ex.getMessage());
    }
}
