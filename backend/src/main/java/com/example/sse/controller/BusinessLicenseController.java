package com.example.sse.controller;

import com.example.sse.domain.BusinessLicense;
import com.example.sse.sse.SseEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 샘플용 인메모리 저장소 (DB 대신)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/business-licenses")
public class BusinessLicenseController {

    private final SseEventPublisher sseEventPublisher;
    private final Map<Long, List<BusinessLicense>> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @GetMapping
    public List<BusinessLicense> find(@RequestHeader("X-Company-Id") Long companyId) {
        return store.getOrDefault(companyId, List.of());
    }

    @PostMapping
    public BusinessLicense add(@RequestHeader("X-Company-Id") Long companyId,
                               @RequestHeader("X-User-Id") Long userId,
                               @RequestBody Map<String, String> body) {
        BusinessLicense license = new BusinessLicense(sequence.incrementAndGet(), companyId, body.get("name"), userId);
        store.computeIfAbsent(companyId, k -> new CopyOnWriteArrayList<>()).add(license);

        // 저장(커밋) 이후 발행
        sseEventPublisher.publish(companyId, "business-license-created", license);
        return license;
    }
}
