# SSE Notification Sample

같은 회사(companyId) 사용자에게 실시간 알림을 보내는 SSE 샘플.
서버가 여러 대여도 Redis Pub/Sub 으로 모든 인스턴스에 전파된다.

## 구조

```
사업자 등록 POST ─▶ [서버 A] 저장 → Redis PUBLISH sse:company-events
                                          │
                     ┌────────────────────┴────────────────────┐
                     ▼                                         ▼
              [서버 A] SUBSCRIBE                         [서버 B] SUBSCRIBE
              emitters.get(companyId)                    emitters.get(companyId)
                     │                                         │
              같은 회사 브라우저들 ◀── SSE event ──▶ 같은 회사 브라우저들
```

| 경로 | 역할 |
|---|---|
| `backend/.../sse/SseEmitterService` | 이 서버에 연결된 Emitter 를 `companyId → (emitterId → SseEmitter)` 로 관리 |
| `backend/.../sse/SseEventPublisher` | Redis 채널 `sse:company-events` 로 발행 |
| `backend/.../sse/SseEventSubscriber` | Redis 메시지 수신 → 해당 companyId 연결에만 전송 |
| `backend/.../config/RedisConfig` | `RedisMessageListenerContainer` 로 채널 구독 |
| `backend/.../controller/NotificationController` | `GET /notification/stream` (SSE 연결) |
| `backend/.../controller/BusinessLicenseController` | `GET/POST /business-licenses` (인메모리 저장 + 발행) |
| `backend/.../exception/GlobalExceptionHandler` | 끊긴 연결 전송 실패(`AsyncRequestNotUsableException`) 무시 |
| `frontend/src/useNotificationStream.ts` | fetch 스트림으로 SSE 파싱 + 3초 후 자동 재연결 |
| `frontend/src/NotificationBell.tsx` | 벨/배지/드롭다운/토스트 |

## 실행

요구사항: Java 21, Node 18+, Docker (Redis 용)

```bash
docker compose up -d                 # Redis :6379

cd backend
./gradlew bootRun                    # :8080

cd frontend
npm install
npm run dev                          # :5173 (/api → :8080 프록시)
```

브라우저 두 탭에서 같은 회사, 다른 사용자로 접속 후 한쪽에서 사업자 등록:

- http://localhost:5173/?companyId=1&userId=1
- http://localhost:5173/?companyId=1&userId=2
- http://localhost:5173/?companyId=2&userId=3  ← 다른 회사: 알림 안 옴

### 다중 인스턴스 확인

```bash
./gradlew bootRun --args='--server.port=8081'                # 두 번째 서버
VITE_API_TARGET=http://localhost:8081 npx vite --port 5174   # 두 번째 프론트
```

5173(→8080) 에서 등록해도 5174(→8081) 탭에 알림이 온다.

### curl 로 확인

```bash
curl -N -H "X-Company-Id: 1" -H "X-User-Id: 1" localhost:8080/notification/stream
curl -X POST -H "X-Company-Id: 1" -H "X-User-Id: 2" -H "Content-Type: application/json" \
     -d '{"name":"테스트"}' localhost:8080/business-licenses
```

### redis-cli

```bash
PUBSUB NUMSUB sse:company-events   # 구독 중인 서버 수 (브라우저 연결 수 아님)
SUBSCRIBE sse:company-events       # 발행 메시지 실시간 확인
```

## 실서비스 적용 시 주의

- **인증**: 샘플은 `X-Company-Id` 헤더를 그대로 믿는다. 실제로는 JWT 등 서버가 검증한 값에서 companyId 를 꺼내야 한다.
- **Spring Security 사용 시**: SSE 종료 시점의 ASYNC 디스패치가 익명으로 인가 거부되지 않도록
  `.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()` 필요.
- **EventSource 대신 fetch**: 브라우저 `EventSource` 는 `Authorization` 헤더를 못 보낸다.
- **프록시 타임아웃**: ALB / nginx 기본 유휴 타임아웃 60초 → 하트비트(주석 이벤트) 주기 전송 권장.
  nginx 는 `proxy_buffering off;` 필요.
- **유실**: Pub/Sub 은 저장하지 않으므로 재연결 사이 이벤트는 유실된다.
  알림함이 필요하면 DB 저장 + 조회 API 를 원본으로 두고 SSE 는 "갱신 신호" 로만 사용.
- **끊긴 연결**: 서버는 쓰기 시점에야 끊김을 안다 (새로고침 직후 전송 실패 WARN 은 정상).
