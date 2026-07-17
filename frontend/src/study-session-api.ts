import { apiFetch, requireSuccess } from "./auth-api";

export type StudySession = {
  id: string;
  origin: "CYCLE" | "FREE";
  status: "ACTIVE" | "PAUSED";
  subject: { id: string; name: string };
  content: { id: string; name: string } | null;
  cycle: {
    id: string;
    name: string;
    runId: string;
    runNumber: number;
    stageId: string;
    stagePosition: number;
    targetMinutes: number;
  } | null;
  startedAt: string;
  measuredSeconds: number;
  serverNow: string;
};

export type StartStudySessionInput =
  | { origin: "CYCLE"; cycleId: string; contentId?: string }
  | { origin: "FREE"; subjectId: string; contentId?: string };

export async function loadCurrentStudySession(): Promise<StudySession | null> {
  const response = await apiFetch("/api/study-sessions/current");
  await requireSuccess(response, "Não foi possível recuperar seu cronômetro.");
  if (response.status === 204) return null;
  return response.json() as Promise<StudySession>;
}

export async function startStudySession(input: StartStudySessionInput): Promise<StudySession> {
  const response = await apiFetch("/api/study-sessions", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input)
  });
  await requireSuccess(response, "Não foi possível iniciar o cronômetro.");
  return response.json() as Promise<StudySession>;
}

async function transitionStudySession(id: string, action: "pause" | "resume"): Promise<StudySession> {
  const response = await apiFetch(`/api/study-sessions/${id}/${action}`, { method: "POST" });
  await requireSuccess(
    response,
    action === "pause" ? "Não foi possível pausar o cronômetro." : "Não foi possível retomar o cronômetro."
  );
  return response.json() as Promise<StudySession>;
}

export function pauseStudySession(id: string): Promise<StudySession> {
  return transitionStudySession(id, "pause");
}

export function resumeStudySession(id: string): Promise<StudySession> {
  return transitionStudySession(id, "resume");
}
