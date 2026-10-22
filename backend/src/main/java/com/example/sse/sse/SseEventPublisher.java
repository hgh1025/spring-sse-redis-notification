package com.example.sse.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseEventPublisher {
    public static final String COMPANY_CHANNEL = "sse:company-events";

    private final StringRedisTemplate stringRedisTemplate;
    private final JsonMapper jsonMapper;

    public void publish(Long companyId, String eventName, Object data) {
        // 알림 실패가 본 요청을 실패시키지 않도록 예외를 삼킨다
        try {
            stringRedisTemplate.convertAndSend(COMPANY_CHANNEL, jsonMapper.writeValueAsString(new SseEventMessage(companyId, eventName, data)));
        } catch (Exception e) {
            log.error("SSE 이벤트 발행 실패: companyId={}, eventName={}", companyId, eventName, e);
        }
    }
}
