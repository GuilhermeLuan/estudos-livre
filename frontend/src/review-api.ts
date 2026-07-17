import { apiFetch, requireSuccess } from "./auth-api";
import type { StudySession } from "./study-session-api";

export type ReviewTiming = "OVERDUE" | "TODAY" | "FUTURE";

export type ReviewOccurrence = {
  occurrenceId: string;
  planId: string;
  subjectId: string;
  subjectName: string;
  contentId: string;
  contentName: string;
  initialStudyDate: string;
  intervalDays: number;
  dueDate: string;
  timing: ReviewTiming;
};

export async function listReviews(): Promise<ReviewOccurrence[]> {
  const response = await apiFetch("/api/reviews");
  await requireSuccess(response, "Não foi possível consultar suas revisões.");
  return response.json() as Promise<ReviewOccurrence[]>;
}

export async function startReview(occurrenceId: string): Promise<StudySession> {
  const response = await apiFetch(`/api/reviews/${occurrenceId}/start`, { method: "POST" });
  await requireSuccess(response, "Não foi possível iniciar esta revisão.");
  return response.json() as Promise<StudySession>;
}
