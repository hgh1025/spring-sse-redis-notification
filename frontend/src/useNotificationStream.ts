import { useEffect, useRef } from 'react';
import { headers, Session } from './api';

export interface SseEvent {
  event: string;
  data: string;
}

// EventSource 는 커스텀 헤더(Authorization 등)를 못 보내므로 fetch 스트림을 직접 파싱한다
export function useNotificationStream(session: Session, onEvent: (e: SseEvent) => void) {
  const handler = useRef(onEvent);
  handler.current = onEvent;

  useEffect(() => {
    const ctrl = new AbortController();
    let timer: number | undefined;

    const connect = async () => {
      try {
        const res = await fetch('/api/notification/stream', {
          headers: { ...headers(session), Accept: 'text/event-stream' },
          signal: ctrl.signal,
        });
        if (res.ok && res.body) await read(res.body, (e) => handler.current(e));
      } catch {
        // 네트워크 끊김 → 아래에서 재연결
      }
      if (!ctrl.signal.aborted) timer = window.setTimeout(connect, 3000);
    };

    connect();
    return () => {
      ctrl.abort();
      clearTimeout(timer);
    };
  }, [session.companyId, session.userId]);
}

async function read(body: ReadableStream<Uint8Array>, emit: (e: SseEvent) => void) {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buf = '';
  for (;;) {
    const { value, done } = await reader.read();
    if (done) return;
    buf += decoder.decode(value, { stream: true });
    let i;
    while ((i = buf.indexOf('\n\n')) >= 0) {
      const block = buf.slice(0, i);
      buf = buf.slice(i + 2);
      let event = 'message';
      const data: string[] = [];
      for (const line of block.split('\n')) {
        if (line.startsWith('event:')) event = line.slice(6).trim();
        else if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''));
      }
      if (data.length) emit({ event, data: data.join('\n') });
    }
  }
}
