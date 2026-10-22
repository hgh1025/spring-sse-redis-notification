import { useEffect, useRef, useState } from 'react';
import type { BusinessLicense, Session } from './api';
import { useNotificationStream } from './useNotificationStream';

interface Notice {
  id: number;
  message: string;
  at: string;
  read: boolean;
}

export default function NotificationBell({ session, onLicenseCreated }: { session: Session; onLicenseCreated: () => void }) {
  const ref = useRef<HTMLDivElement>(null);
  const [list, setList] = useState<Notice[]>([]);
  const [open, setOpen] = useState(false);
  const [toast, setToast] = useState<Notice | null>(null);

  useNotificationStream(session, ({ event, data }) => {
    console.debug('[SSE]', event, data);
    if (event !== 'business-license-created') return;
    const license: BusinessLicense = JSON.parse(data);
    const notice = { id: Date.now(), message: `'${license.name}' 사업자가 등록되었습니다. (user ${license.createId})`, at: new Date().toLocaleTimeString(), read: false };
    setList((l) => [notice, ...l].slice(0, 50));
    setToast(notice);
    onLicenseCreated();
  });

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 4000);
    return () => clearTimeout(t);
  }, [toast]);

  useEffect(() => {
    if (!open) return;
    const close = (e: MouseEvent) => !ref.current?.contains(e.target as Node) && setOpen(false);
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, [open]);

  const unread = list.filter((n) => !n.read).length;
  const toggle = () => {
    setOpen((o) => !o);
    setList((l) => l.map((n) => ({ ...n, read: true })));
  };

  return (
    <div className="noti" ref={ref}>
      <button className="noti__bell" onClick={toggle} aria-label="알림">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
        {unread > 0 && <span className="noti__badge">{unread > 99 ? '99+' : unread}</span>}
      </button>
      {open && (
        <div className="noti__panel">
          <div className="noti__head">알림</div>
          {list.length ? (
            list.map((n) => (
              <div key={n.id} className="noti__item">
                <p>{n.message}</p>
                <time>{n.at}</time>
              </div>
            ))
          ) : (
            <div className="noti__empty">새 알림이 없습니다.</div>
          )}
        </div>
      )}
      {toast && (
        <div className="noti__toast" onClick={() => setToast(null)}>
          {toast.message}
        </div>
      )}
    </div>
  );
}
