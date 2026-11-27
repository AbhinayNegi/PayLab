export type Profile = {
  id: string;
  fullName: string;
  email: string;
  role: string;
  createdAt: string;
};

export type Session = { accessToken: string; tokenType: string; expiresIn: number };

export class ApiError extends Error {
  constructor(readonly status: number, message: string, readonly fields: Record<string, string>) {
    super(message);
  }
}

export async function api<T>(path: string, options: { body?: object; token?: string; signal?: AbortSignal } = {}) {
  const headers = new Headers({ Accept: 'application/json' });
  if (options.body) headers.set('Content-Type', 'application/json');
  if (options.token) headers.set('Authorization', 'Bearer ' + options.token);
  const response = await fetch('/api/v1' + path, {
    method: options.body ? 'POST' : 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
    signal: options.signal,
  });
  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new ApiError(response.status, error?.message ?? 'The service could not complete your request.',
      error?.fieldErrors ?? {});
  }
  return await response.json() as T;
}
