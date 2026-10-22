import { FormEvent, useCallback, useEffect, useState } from 'react';
import { addLicense, BusinessLicense, findLicenses, Session } from './api';
import NotificationBell from './NotificationBell';

// URL 쿼리로 세션 지정: /?companyId=8&userId=86 (탭마다 다른 사용자로 테스트)
const initial = (): Session => {
  const q = new URLSearchParams(location.search);
  return { companyId: Number(q.get('companyId') ?? 1), userId: Number(q.get('userId') ?? 1) };
};

export default function App() {
  const [session] = useState(initial);
  const [licenses, setLicenses] = useState<BusinessLicense[]>([]);
  const [name, setName] = useState('');

  const reload = useCallback(() => findLicenses(session).then(setLicenses), [session]);
  useEffect(() => {
    reload();
  }, [reload]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    await addLicense(session, name.trim());
    setName('');
    reload();
  };

  return (
    <>
      <header className="header">
        <strong>SSE Sample</strong>
        <span className="header__session">
          company <b>{session.companyId}</b> / user <b>{session.userId}</b>
        </span>
        <NotificationBell session={session} onLicenseCreated={reload} />
      </header>
      <main className="main">
        <form className="form" onSubmit={submit}>
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="사업자명" />
          <button type="submit">사업자 등록</button>
        </form>
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>사업자명</th>
              <th>등록자</th>
            </tr>
          </thead>
          <tbody>
            {licenses.length ? (
              licenses.map((l) => (
                <tr key={l.id}>
                  <td>{l.id}</td>
                  <td>{l.name}</td>
                  <td>user {l.createId}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={3} className="table__empty">등록된 사업자가 없습니다.</td>
              </tr>
            )}
          </tbody>
        </table>
      </main>
    </>
  );
}
