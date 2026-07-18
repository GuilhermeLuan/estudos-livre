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

export type ReviewPlanStatus = "ACTIVE" | "CANCELED";

export type ReviewPlanSummary = {
  id: string;
  status: ReviewPlanStatus;
  version: number;
  subjectName: string;
  contentName: string;
  initialStudyDate: string;
  scheduledCount: number;
  completedCount: number;
  skippedCount: number;
  canceledCount: number;
};

export type ReviewPlanOccurrenceStatus = "SCHEDULED" | "COMPLETED" | "SKIPPED" | "CANCELED";

export type ReviewPlan = {
  id: string;
  status: ReviewPlanStatus;
  version: number;
  subject: { id: string; name: string };
  content: { id: string; name: string };
  initialStudyDate: string;
  occurrences: Array<{
    id: string;
    intervalDays: number;
    dueDate: string;
    status: ReviewPlanOccurrenceStatus;
    resolvedAt: string | null;
    inProgress: boolean;
  }>;
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

export async function listReviewPlans(): Promise<ReviewPlanSummary[]> {
  const response = await apiFetch("/api/review-plans");
  await requireSuccess(response, "Não foi possível consultar seus planos de revisão.");
  return response.json() as Promise<ReviewPlanSummary[]>;
}

export async function getReviewPlan(planId: string): Promise<ReviewPlan> {
  const response = await apiFetch(`/api/review-plans/${planId}`);
  await requireSuccess(response, "Não foi possível consultar este plano de revisão.");
  return response.json() as Promise<ReviewPlan>;
}

export async function updateReviewSchedule(
  planId: string,
  expectedVersion: number,
  occurrences: Array<{ occurrenceId: string; dueDate: string }>
): Promise<ReviewPlan> {
  const response = await apiFetch(`/api/review-plans/${planId}/schedule`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ expectedVersion, occurrences })
  });
  await requireSuccess(response, "Não foi possível salvar as novas datas.");
  return response.json() as Promise<ReviewPlan>;
}

export async function cancelReviewPlan(planId: string, expectedVersion: number): Promise<ReviewPlan> {
  const response = await apiFetch(`/api/review-plans/${planId}/cancel`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ expectedVersion })
  });
  await requireSuccess(response, "Não foi possível cancelar este plano.");
  return response.json() as Promise<ReviewPlan>;
}

export async function reactivateReviewPlan(planId: string, expectedVersion: number): Promise<ReviewPlan> {
  const response = await apiFetch(`/api/review-plans/${planId}/reactivate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ expectedVersion })
  });
  await requireSuccess(response, "Não foi possível reativar este plano.");
  return response.json() as Promise<ReviewPlan>;
}
