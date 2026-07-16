export type CurrentIdentity = {
  id: string;
  email: string;
  timeZone: string;
};

export type AuthSnapshot =
  | { state: "bootstrap" }
  | { state: "login" }
  | { state: "authenticated"; identity: CurrentIdentity };

type ProblemDetails = {
  detail?: string;
  errors?: Record<string, string>;
};

export class ApiError extends Error {
  constructor(message: string, readonly fieldErrors: Record<string, string> = {}) {
    super(message);
  }
}

function csrfToken() {
  const cookie = document.cookie
    .split(";")
    .map((value) => value.trim())
    .find((value) => value.startsWith("XSRF-TOKEN="));

  return cookie ? decodeURIComponent(cookie.slice("XSRF-TOKEN=".length)) : undefined;
}

async function apiFetch(path: string, init: RequestInit = {}) {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");

  if (init.method && !["GET", "HEAD"].includes(init.method)) {
    const token = csrfToken();
    if (token) headers.set("X-XSRF-TOKEN", token);
  }

  return fetch(path, { ...init, headers, credentials: "same-origin" });
}

async function requireSuccess(response: Response, fallbackMessage: string) {
  if (response.ok) return;

  let problem: ProblemDetails = {};
  if (response.headers.get("Content-Type")?.includes("json")) {
    problem = await response.json() as ProblemDetails;
  }
  throw new ApiError(problem.detail ?? fallbackMessage, problem.errors);
}

export async function loadAuthSnapshot(): Promise<AuthSnapshot> {
  const bootstrapResponse = await apiFetch("/api/auth/bootstrap-status");
  await requireSuccess(bootstrapResponse, "Não foi possível consultar a instalação.");
  const bootstrap = await bootstrapResponse.json() as { registrationRequired: boolean };
  if (bootstrap.registrationRequired) return { state: "bootstrap" };

  const identityResponse = await apiFetch("/api/auth/me");
  if (identityResponse.status === 401) return { state: "login" };
  await requireSuccess(identityResponse, "Não foi possível consultar a sessão.");
  return { state: "authenticated", identity: await identityResponse.json() as CurrentIdentity };
}

export async function createInitialAccount(input: {
  email: string;
  password: string;
  timeZone: string;
}) {
  const response = await apiFetch("/api/auth/bootstrap", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input)
  });
  await requireSuccess(response, "Não foi possível criar a primeira conta.");
}

export async function login(email: string, password: string): Promise<CurrentIdentity> {
  const body = new URLSearchParams({ email, password });
  const response = await apiFetch("/api/auth/login", { method: "POST", body });
  await requireSuccess(response, "E-mail ou senha inválidos.");

  const identityResponse = await apiFetch("/api/auth/me");
  await requireSuccess(identityResponse, "A sessão foi iniciada, mas não pôde ser consultada.");
  return identityResponse.json() as Promise<CurrentIdentity>;
}

export async function logout() {
  const response = await apiFetch("/api/auth/logout", { method: "POST" });
  await requireSuccess(response, "Não foi possível encerrar a sessão.");
}
