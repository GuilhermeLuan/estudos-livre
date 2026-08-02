package br.com.estudalivre.review.controller;

import br.com.estudalivre.review.service.ReviewOccurrenceNotAvailableException;
import br.com.estudalivre.review.service.ReviewPlanNotFoundException;
import br.com.estudalivre.review.service.ReviewPlanConflictException;
import br.com.estudalivre.studysession.service.OpenStudySessionAlreadyExistsException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {ReviewController.class, ReviewPlanController.class})
public class ReviewExceptionHandler {

    @ExceptionHandler(ReviewPlanNotFoundException.class)
    ProblemDetail planNotFound(ReviewPlanNotFoundException exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Plano de revisão não encontrado",
                "review-plan-not-found",
                exception);
    }

    @ExceptionHandler(ReviewPlanConflictException.class)
    ProblemDetail planConflict(ReviewPlanConflictException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "Plano de revisão desatualizado",
                "review-plan-conflict",
                exception);
    }

    @ExceptionHandler(ReviewOccurrenceNotAvailableException.class)
    ProblemDetail occurrenceNotAvailable(ReviewOccurrenceNotAvailableException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "Revisão indisponível",
                "review-occurrence-not-available",
                exception);
    }

    @ExceptionHandler(OpenStudySessionAlreadyExistsException.class)
    ProblemDetail sessionAlreadyOpen(OpenStudySessionAlreadyExistsException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "Cronômetro já em andamento",
                "study-session-already-open",
                exception);
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String type,
            RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(title);
        problem.setType(URI.create("https://estudalivre.local/problems/" + type));
        return problem;
    }
}
