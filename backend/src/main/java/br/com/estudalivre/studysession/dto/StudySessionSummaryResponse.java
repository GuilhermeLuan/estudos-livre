package br.com.estudalivre.studysession.dto;

import br.com.estudalivre.studysession.model.ExerciseResult;
import br.com.estudalivre.studysession.repository.StudySessionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StudySessionSummaryResponse(
        List<SubjectSummary> subjects,
        List<ContentSummary> contents) {

    public record SubjectReference(UUID id, String name) {
    }

    public record ContentReference(UUID id, String name) {
    }

    public record SubjectSummary(
            SubjectReference subject,
            long effectiveSeconds,
            long questionsAttempted,
            long questionsCorrect,
            BigDecimal accuracyPercentage) {
    }

    public record ContentSummary(
            ContentReference content,
            SubjectReference subject,
            long effectiveSeconds,
            long questionsAttempted,
            long questionsCorrect,
            BigDecimal accuracyPercentage) {
    }

    public static StudySessionSummaryResponse from(
            List<StudySessionRepository.SubjectSessionAggregate> subjects,
            List<StudySessionRepository.ContentSessionAggregate> contents) {
        return new StudySessionSummaryResponse(
                subjects.stream().map(row -> new SubjectSummary(
                        new SubjectReference(row.subjectId(), row.subjectName()),
                        row.effectiveSeconds(), row.questionsAttempted(), row.questionsCorrect(),
                        accuracy(row.questionsAttempted(), row.questionsCorrect()))).toList(),
                contents.stream().map(row -> new ContentSummary(
                        new ContentReference(row.contentId(), row.contentName()),
                        new SubjectReference(row.subjectId(), row.subjectName()),
                        row.effectiveSeconds(), row.questionsAttempted(), row.questionsCorrect(),
                        accuracy(row.questionsAttempted(), row.questionsCorrect()))).toList());
    }

    private static BigDecimal accuracy(long attempted, long correct) {
        return attempted == 0 ? null : ExerciseResult.accuracyPercentage(attempted, correct);
    }
}
