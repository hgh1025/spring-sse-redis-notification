package com.example.sse.sse;

public record SseEventMessage(Long companyId, String eventName, Object data) {
}
