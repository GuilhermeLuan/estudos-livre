import { apiFetch, requireSuccess } from "./auth-api";

export type SubjectStatus = "active" | "archived";

export type Subject = {
  id: string;
  name: string;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
};

export async function listSubjects(status: SubjectStatus): Promise<Subject[]> {
  const response = await apiFetch(`/api/subjects?status=${status}`);
  await requireSuccess(response, "Não foi possível consultar suas matérias.");
  return response.json() as Promise<Subject[]>;
}

export async function getSubject(id: string): Promise<Subject> {
  const response = await apiFetch(`/api/subjects/${id}`);
  await requireSuccess(response, "Não foi possível consultar a matéria.");
  return response.json() as Promise<Subject>;
}

export async function createSubject(name: string): Promise<Subject> {
  const response = await apiFetch("/api/subjects", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name })
  });
  await requireSuccess(response, "Não foi possível criar a matéria.");
  return response.json() as Promise<Subject>;
}

export async function updateSubject(id: string, name: string): Promise<Subject> {
  const response = await apiFetch(`/api/subjects/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name })
  });
  await requireSuccess(response, "Não foi possível atualizar a matéria.");
  return response.json() as Promise<Subject>;
}

export async function archiveSubject(id: string): Promise<Subject> {
  const response = await apiFetch(`/api/subjects/${id}/archive`, { method: "POST" });
  await requireSuccess(response, "Não foi possível arquivar a matéria.");
  return response.json() as Promise<Subject>;
}

export async function restoreSubject(id: string): Promise<Subject> {
  const response = await apiFetch(`/api/subjects/${id}/restore`, { method: "POST" });
  await requireSuccess(response, "Não foi possível restaurar a matéria.");
  return response.json() as Promise<Subject>;
}
