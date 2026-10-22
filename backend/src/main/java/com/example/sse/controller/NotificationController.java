package com.example.sse.controller;

import com.example.sse.sse.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 샘플에서는 인증 대신 헤더로 companyId/userId를 받는다
 * 실제 서비스에서는 JWT 등 서버가 검증한 값을 사용해야 한다 (헤더 값은 위조 가능)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {

    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestHeader("X-Company-Id") Long companyId,
                                @RequestHeader("X-User-Id") Long userId) {
        return sseEmitterService.subscribe(companyId, userId);
    }
}
