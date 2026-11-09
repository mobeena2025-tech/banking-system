// Small fetch wrapper: adds the token, parses JSON, and turns errors into readable messages.

const TOKEN_KEY = "bank.token";

export const auth = {
  get token() {
    try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
  },
  set token(value) {
    try {
      value ? localStorage.setItem(TOKEN_KEY, value) : localStorage.removeItem(TOKEN_KEY);
    } catch { /* storage unavailable */ }
  },
};

export class ApiError extends Error {
  constructor(status, message, fields) {
    super(message);
    this.status = status;
    this.fields = fields || {};
  }
}

export async function api(path, { method = "GET", body } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (auth.token) headers.Authorization = `Bearer ${auth.token}`;

  const res = await fetch(`/api${path}`, { method, headers, body: body ? JSON.stringify(body) : undefined });
  const data = res.status === 204 ? null : await res.json().catch(() => null);

  if (res.status === 401 && auth.token) {
    auth.token = null;
    window.location.href = "/login";
  }
  if (!res.ok) {
    throw new ApiError(res.status, data?.detail || `Request failed (${res.status})`, data?.errors);
  }
  return data;
}

export const money = (n) =>
  new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(Number(n || 0));
