// 샘플용: 인증 대신 헤더로 companyId / userId 전달
export interface Session {
  companyId: number;
  userId: number;
}

export interface BusinessLicense {
  id: number;
  companyId: number;
  name: string;
  createId: number;
}

export const headers = (s: Session) => ({ 'X-Company-Id': String(s.companyId), 'X-User-Id': String(s.userId) });

export const findLicenses = (s: Session): Promise<BusinessLicense[]> =>
  fetch('/api/business-licenses', { headers: headers(s) }).then((r) => r.json());

export const addLicense = (s: Session, name: string): Promise<BusinessLicense> =>
  fetch('/api/business-licenses', {
    method: 'POST',
    headers: { ...headers(s), 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  }).then((r) => r.json());
