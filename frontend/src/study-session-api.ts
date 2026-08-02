import { apiFetch, requireSuccess } from "./auth-api";

export type StudySession = {
  id: string;
  origin: "CYCLE" | "FREE" | "MANUAL" | "REVIEW";
  status: "ACTIVE" | "PAUSED" | "FINISHED";
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
  notes: string | null;
  measuredSeconds: number;
  effectiveSeconds: number | null;
  finishedAt: string | null;
  version: number;
  exerciseResult: ExerciseResult | null;
  credits: Array<{
    runStageId: string;
    cycleId: string;
    runId: string;
    cycleStageId: string | null;
    stagePosition: number;
    creditedSeconds: number;
  }>;
  serverNow: string;
};

export type ExerciseResult = {
  questionsAttempted: number;
  questionsCorrect: number;
  accuracyPercentage: number;
};

export type ExerciseResultInput = {
  questionsAttempted: number;
  questionsCorrect: number;
};

export type StudySessionSummary = {
  subjects: Array<{
    subject: { id: string; name: string };
    effectiveSeconds: number;
    questionsAttempted: number;
    questionsCorrect: number;
    accuracyPercentage: number | null;
  }>;
  contents: Array<{
    content: { id: string; name: string };
    subject: { id: string; name: string };
    effectiveSeconds: number;
    questionsAttempted: number;
    questionsCorrect: number;
    accuracyPercentage: number | null;
  }>;
};

export type UpdateStudySessionInput = {
  expectedVersion: number;
  startedAtLocal: string;
  effectiveSeconds: number;
  subjectId: string;
  contentId?: string;
  notes?: string | null;
  questionsAttempted?: number;
  questionsCorrect?: number;
};

export type StartStudySessionInput =
  | { origin: "CYCLE"; cycleId: string; contentId?: string }
  | { origin: "FREE"; subjectId: string; contentId?: string };

export type CreateManualStudySessionInput = {
  startedAtLocal: string;
  effectiveSeconds: number;
  subjectId: string;
  contentId?: string;
  notes?: string;
};

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

export async function createManualStudySession(
  input: CreateManualStudySessionInput
): Promise<StudySession> {
  const response = await apiFetch("/api/study-sessions/manual", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input)
  });
  await requireSuccess(response, "Não foi possível registrar o estudo concluído.");
  return response.json() as Promise<StudySession>;
}

export async function listStudySessionHistory(): Promise<StudySession[]> {
  const response = await apiFetch("/api/study-sessions/history");
  await requireSuccess(response, "Não foi possível consultar o histórico de estudos.");
  return response.json() as Promise<StudySession[]>;
}

export async function loadStudySessionSummary(): Promise<StudySessionSummary> {
  const response = await apiFetch("/api/study-sessions/summary");
  await requireSuccess(response, "Não foi possível calcular o resumo de estudos.");
  return response.json() as Promise<StudySessionSummary>;
}

export async function updateStudySession(
  id: string,
  input: UpdateStudySessionInput
): Promise<StudySession> {
  const response = await apiFetch(`/api/study-sessions/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input)
  });
  await requireSuccess(response, "Não foi possível atualizar a sessão.");
  return response.json() as Promise<StudySession>;
}

export async function deleteStudySession(id: string, expectedVersion: number): Promise<void> {
  const response = await apiFetch(`/api/study-sessions/${id}`, {
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ expectedVersion })
  });
  await requireSuccess(response, "Não foi possível excluir a sessão.");
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

export async function finishStudySession(
  id: string,
  effectiveSeconds: number,
  expectedVersion: number,
  exerciseResult?: ExerciseResultInput,
  scheduleReviews = false
): Promise<StudySession> {
  const response = await apiFetch(`/api/study-sessions/${id}/finish`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ effectiveSeconds, expectedVersion, ...exerciseResult, scheduleReviews })
  });
  await requireSuccess(response, "Não foi possível finalizar a sessão.");
  return response.json() as Promise<StudySession>;
}
