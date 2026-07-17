package br.com.estudalivre.studysession.controller;

import br.com.estudalivre.studysession.service.InvalidStudySessionTransitionException;
import br.com.estudalivre.studysession.service.OpenStudySessionAlreadyExistsException;
import br.com.estudalivre.studysession.service.StudyCycleIsNotActiveException;
import br.com.estudalivre.studysession.service.StudySessionNotFoundException;
import br.com.estudalivre.studycycle.service.StudyCycleNotFoundException;
import br.com.estudalivre.subject.service.ContentNotFoundException;
import br.com.estudalivre.subject.service.SubjectNotFoundException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = StudySessionController.class)
public class StudySessionExceptionHandler {

    @ExceptionHandler(StudySessionNotFoundException.class)
    ProblemDetail sessionNotFound(StudySessionNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Sessão de estudo não encontrada", "study-session-not-found", exception);
    }

    @ExceptionHandler(OpenStudySessionAlreadyExistsException.class)
    ProblemDetail sessionAlreadyOpen(OpenStudySessionAlreadyExistsException exception) {
        return problem(HttpStatus.CONFLICT, "Cronômetro já em andamento", "study-session-already-open", exception);
    }

    @ExceptionHandler(InvalidStudySessionTransitionException.class)
    ProblemDetail invalidTransition(InvalidStudySessionTransitionException exception) {
        return problem(HttpStatus.CONFLICT, "Transição de sessão inválida", "invalid-study-session-transition", exception);
    }

    @ExceptionHandler(StudyCycleIsNotActiveException.class)
    ProblemDetail cycleInactive(StudyCycleIsNotActiveException exception) {
        return problem(HttpStatus.CONFLICT, "Ciclo não está ativo", "study-cycle-not-active", exception);
    }

    @ExceptionHandler(StudyCycleNotFoundException.class)
    ProblemDetail cycleNotFound(StudyCycleNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Ciclo não encontrado", "study-cycle-not-found", exception);
    }

    @ExceptionHandler(SubjectNotFoundException.class)
    ProblemDetail subjectNotFound(SubjectNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Matéria não encontrada", "subject-not-found", exception);
    }

    @ExceptionHandler(ContentNotFoundException.class)
    ProblemDetail contentNotFound(ContentNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Conteúdo não encontrado", "content-not-found", exception);
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
