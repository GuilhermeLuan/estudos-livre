import { apiFetch, requireSuccess } from "./auth-api";

export type StudyCycleStage = {
  id: string;
  position: number;
  subjectId: string;
  subjectName: string;
  targetMinutes: number;
  longBlockWarning: boolean;
};

export type StudyCycle = {
  id: string;
  name: string;
  mode: "CUSTOM" | "SUGGESTED";
  status: "DRAFT" | "INACTIVE" | "ACTIVE";
  totalMinutes: number;
  activatable: boolean;
  currentRun: {
    id: string;
    number: number;
    status: "IN_PROGRESS" | "PAUSED";
    startedAt: string;
  } | null;
  suggestion?: {
    totalMinutes: number;
    durationRule: string;
    priorityRule: string;
    subjects: Array<{
      subjectId: string;
      subjectName: string;
      questionCount: number;
      weight: number;
      difficulty: StudyCycleDifficulty;
      priority: number;
      allocatedMinutes: number;
      appearanceCount: number;
    }>;
  } | null;
  stages: StudyCycleStage[];
  createdAt: string;
  updatedAt: string;
};

export type StudyCycleStageInput = {
  subjectId: string;
  targetMinutes: number;
};

export type CycleSwitchAction = "PAUSE" | "ABANDON";
export type StudyCycleDifficulty = "EASY" | "MEDIUM" | "HARD";

export type SuggestedStudyCycleSubjectInput = {
  subjectId: string;
  questionCount: number;
  weight: number;
  difficulty: StudyCycleDifficulty;
};

export async function listStudyCycles(): Promise<StudyCycle[]> {
  const response = await apiFetch("/api/study-cycles");
  await requireSuccess(response, "Não foi possível consultar seus ciclos.");
  return response.json() as Promise<StudyCycle[]>;
}

export async function createStudyCycle(name: string): Promise<StudyCycle> {
  const response = await apiFetch("/api/study-cycles", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name })
  });
  await requireSuccess(response, "Não foi possível criar o ciclo.");
  return response.json() as Promise<StudyCycle>;
}

export async function createSuggestedStudyCycle(
  name: string,
  subjects: SuggestedStudyCycleSubjectInput[]
): Promise<StudyCycle> {
  const response = await apiFetch("/api/study-cycles/suggestions", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name, subjects })
  });
  await requireSuccess(response, "Não foi possível gerar o ciclo sugerido.");
  return response.json() as Promise<StudyCycle>;
}

export async function updateStudyCycle(
  id: string,
  name: string,
  stages: StudyCycleStageInput[]
): Promise<StudyCycle> {
  const response = await apiFetch(`/api/study-cycles/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name, stages })
  });
  await requireSuccess(response, "Não foi possível salvar o ciclo.");
  return response.json() as Promise<StudyCycle>;
}

export async function activateStudyCycle(id: string, currentRunAction?: CycleSwitchAction): Promise<StudyCycle> {
  const response = await apiFetch(`/api/study-cycles/${id}/activate`, {
    method: "POST",
    ...(currentRunAction ? {
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ currentRunAction })
    } : {})
  });
  await requireSuccess(response, "Não foi possível ativar o ciclo.");
  return response.json() as Promise<StudyCycle>;
}
