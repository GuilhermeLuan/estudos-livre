package br.com.estudalivre.studysession.dto;

import br.com.estudalivre.studysession.model.ExerciseResult;
import br.com.estudalivre.studysession.repository.StudySessionRepository.ContentExerciseAggregate;
import br.com.estudalivre.studysession.repository.StudySessionRepository.SubjectExerciseAggregate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExerciseSummaryResponse(
        List<SubjectSummary> subjects,
        List<ContentSummary> contents) {

    public record SubjectSummary(
            UUID subjectId,
            String subjectName,
            long questionsAttempted,
            long questionsCorrect,
            BigDecimal accuracyPercentage) {

        static SubjectSummary from(SubjectExerciseAggregate aggregate) {
            return new SubjectSummary(
                    aggregate.subjectId(),
                    aggregate.subjectName(),
                    aggregate.questionsAttempted(),
                    aggregate.questionsCorrect(),
                    ExerciseResult.accuracyPercentage(
                            aggregate.questionsAttempted(), aggregate.questionsCorrect()));
        }
    }

    public record ContentSummary(
            UUID contentId,
            String contentName,
            UUID subjectId,
            String subjectName,
            long questionsAttempted,
            long questionsCorrect,
            BigDecimal accuracyPercentage) {

        static ContentSummary from(ContentExerciseAggregate aggregate) {
            return new ContentSummary(
                    aggregate.contentId(),
                    aggregate.contentName(),
                    aggregate.subjectId(),
                    aggregate.subjectName(),
                    aggregate.questionsAttempted(),
                    aggregate.questionsCorrect(),
                    ExerciseResult.accuracyPercentage(
                            aggregate.questionsAttempted(), aggregate.questionsCorrect()));
        }
    }

    public static ExerciseSummaryResponse from(
            List<SubjectExerciseAggregate> subjects,
            List<ContentExerciseAggregate> contents) {
        return new ExerciseSummaryResponse(
                subjects.stream().map(SubjectSummary::from).toList(),
                contents.stream().map(ContentSummary::from).toList());
    }
}
