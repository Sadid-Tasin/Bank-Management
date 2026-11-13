import type { AuthResponse, Profile } from "@/lib/types";

const API_BASE = (process.env.NEXT_PUBLIC_API_BASE_URL || "https://bank-management-system-m3th.onrender.com").replace(/\/$/, "");
const SESSION_KEY = "confused_bank_session_v1";

export interface SessionData {
  token: string;
  email: string;
  role: string;
  expiresAt: number;
}

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

function parseError(body: unknown, fallback: string) {
  if (typeof body === "string" && body.trim()) return body;
  if (body && typeof body === "object") {
    const value = body as Record<string, unknown>;
    for (const key of ["message", "detail", "error", "title"]) {
      if (typeof value[key] === "string" && value[key]) return value[key] as string;
    }
    if (Array.isArray(value.errors)) return value.errors.map((item) => typeof item === "string" ? item : JSON.stringify(item)).join(", ");
  }
  return fallback;
}

export async function apiRequest<T>(path: string, token?: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");
  if (options.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);
  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, { ...options, headers, cache: "no-store" });
  } catch {
    throw new ApiError(0, "Cannot reach the banking server. Check your internet connection and API URL.");
  }
  const raw = await response.text();
  let parsed: unknown = null;
  if (raw) {
    try { parsed = JSON.parse(raw); } catch { parsed = raw; }
  }
  if (!response.ok) {
    const fallback = response.status === 401 ? "Your session is invalid or expired. Please sign in again." : `Request failed (${response.status}).`;
    throw new ApiError(response.status, parseError(parsed, fallback));
  }
  return parsed as T;
}

export async function login(email: string, password: string) {
  return apiRequest<AuthResponse>("/api/v1/auth/login", undefined, {
    method: "POST", body: JSON.stringify({ email, password }),
  });
}
export async function registerCustomer(payload: { fullName: string; email: string; password: string; phone?: string; nationalId?: string; address?: string }) {
  return apiRequest<{ userId: number; email: string; role: string; kycStatus: string }>("/api/v1/auth/register", undefined, { method: "POST", body: JSON.stringify(payload) });
}
export async function getProfile(token: string) {
  return apiRequest<Profile>("/api/v1/auth/me", token);
}
export function saveSession(auth: AuthResponse) {
  if (typeof window === "undefined") return;
  const session: SessionData = {
    token: auth.accessToken,
    email: auth.email,
    role: auth.role,
    expiresAt: Date.now() + auth.expiresInSeconds * 1000,
  };
  window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
}
export function readSession(): SessionData | null {
  if (typeof window === "undefined") return null;
  const raw = window.sessionStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    const session = JSON.parse(raw) as SessionData;
    if (!session.token || !session.expiresAt || session.expiresAt <= Date.now()) {
      window.sessionStorage.removeItem(SESSION_KEY);
      return null;
    }
    return session;
  } catch {
    window.sessionStorage.removeItem(SESSION_KEY);
    return null;
  }
}
export function clearSession() {
  if (typeof window !== "undefined") window.sessionStorage.removeItem(SESSION_KEY);
}
