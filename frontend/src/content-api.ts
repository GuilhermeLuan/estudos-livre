import { apiFetch, requireSuccess } from "./auth-api";

export type ContentStatus = "active" | "archived";

export type StudyContent = {
  id: string;
  subjectId: string;
  name: string;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
};

export async function listContents(subjectId: string, status: ContentStatus): Promise<StudyContent[]> {
  const response = await apiFetch(`/api/subjects/${subjectId}/contents?status=${status}`);
  await requireSuccess(response, "Não foi possível consultar os conteúdos desta matéria.");
  return response.json() as Promise<StudyContent[]>;
}

export async function createContent(subjectId: string, name: string): Promise<StudyContent> {
  const response = await apiFetch(`/api/subjects/${subjectId}/contents`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name })
  });
  await requireSuccess(response, "Não foi possível criar o conteúdo.");
  return response.json() as Promise<StudyContent>;
}

export async function updateContent(subjectId: string, contentId: string, name: string): Promise<StudyContent> {
  const response = await apiFetch(`/api/subjects/${subjectId}/contents/${contentId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name })
  });
  await requireSuccess(response, "Não foi possível atualizar o conteúdo.");
  return response.json() as Promise<StudyContent>;
}

async function changeContentState(
  subjectId: string,
  contentId: string,
  action: "archive" | "restore"
): Promise<StudyContent> {
  const response = await apiFetch(`/api/subjects/${subjectId}/contents/${contentId}/${action}`, {
    method: "POST"
  });
  await requireSuccess(
    response,
    action === "archive" ? "Não foi possível arquivar o conteúdo." : "Não foi possível restaurar o conteúdo."
  );
  return response.json() as Promise<StudyContent>;
}

export function archiveContent(subjectId: string, contentId: string): Promise<StudyContent> {
  return changeContentState(subjectId, contentId, "archive");
}

export function restoreContent(subjectId: string, contentId: string): Promise<StudyContent> {
  return changeContentState(subjectId, contentId, "restore");
}
