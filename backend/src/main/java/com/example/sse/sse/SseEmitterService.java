package com.example.sse.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이 인스턴스에 연결된 Emitter를 companyId 단위로 관리
 * 인스턴스 간 전파는 Redis Pub/Sub(SseEventPublisher.COMPANY_CHANNEL)
 */
@Slf4j
@Service
public class SseEmitterService {

    private static final long TIMEOUT = 60L * 60 * 1000; // 1시간

    // companyId -> (emitterId -> emitter)
    private final Map<Long, Map<String, SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long companyId, Long userId) {
        String emitterId = userId + "_" + UUID.randomUUID();
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.computeIfAbsent(companyId, k -> new ConcurrentHashMap<>()).put(emitterId, emitter);
        log.info("SSE 연결: companyId={}, emitterId={}", companyId, emitterId);

        emitter.onCompletion(() -> {
            log.info("SSE 종료: companyId={}, emitterId={}", companyId, emitterId);
            remove(companyId, emitterId);
        });
        emitter.onTimeout(emitter::complete);

        // 503 방지용 초기 이벤트
        send(companyId, emitterId, emitter, "connect", emitterId);
        return emitter;
    }

    public void sendToCompany(SseEventMessage message) {
        Map<String, SseEmitter> companyEmitters = emitters.get(message.companyId());
        if (companyEmitters == null) {
            return;
        }
        companyEmitters.forEach((emitterId, emitter) ->
                send(message.companyId(), emitterId, emitter, message.eventName(), message.data()));
    }

    private void send(Long companyId, String emitterId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data).reconnectTime(3000));
        } catch (Exception e) {
            // 클라이언트가 이미 끊긴 경우 (새로고침, 탭 닫기)
            log.warn("SSE 전송 실패: companyId={}, emitterId={}", companyId, emitterId);
            remove(companyId, emitterId);
        }
    }

    private void remove(Long companyId, String emitterId) {
        emitters.computeIfPresent(companyId, (k, m) -> {
            m.remove(emitterId);
            return m.isEmpty() ? null : m;
        });
    }
}
