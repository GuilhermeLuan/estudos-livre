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
    currentStagePosition: number;
    startedAt: string;
  } | null;
  stages: StudyCycleStage[];
  createdAt: string;
  updatedAt: string;
};

export type StudyCycleStageInput = {
  subjectId: string;
  targetMinutes: number;
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

export async function activateStudyCycle(id: string): Promise<StudyCycle> {
  const response = await apiFetch(`/api/study-cycles/${id}/activate`, {
    method: "POST"
  });
  await requireSuccess(response, "Não foi possível ativar o ciclo.");
  return response.json() as Promise<StudyCycle>;
}
