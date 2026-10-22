package com.example.sse.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseEventSubscriber implements MessageListener {
    private final SseEmitterService sseEmitterService;
    private final JsonMapper jsonMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            SseEventMessage body = jsonMapper.readValue(message.getBody(), SseEventMessage.class);
            switch (channel) {
                case SseEventPublisher.COMPANY_CHANNEL -> sseEmitterService.sendToCompany(body);
                default -> log.warn("처리하지 않는 채널: {}", channel);
            }
        } catch (Exception e) {
            log.error("SSE 이벤트 수신 처리 실패", e);
        }
    }
}
