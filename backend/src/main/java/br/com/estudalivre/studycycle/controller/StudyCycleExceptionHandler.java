package br.com.estudalivre.studycycle.controller;

import br.com.estudalivre.studycycle.service.ActiveStudyCycleCannotBeEmptyException;
import br.com.estudalivre.studycycle.service.StudyCycleNotFoundException;
import br.com.estudalivre.studycycle.service.StudyCycleNotActivatableException;
import br.com.estudalivre.studycycle.service.StudyCycleSwitchDecisionRequiredException;
import br.com.estudalivre.subject.service.SubjectNotFoundException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = StudyCycleController.class)
public class StudyCycleExceptionHandler {

    @ExceptionHandler(StudyCycleNotFoundException.class)
    ProblemDetail cycleNotFound(StudyCycleNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Ciclo não encontrado");
        problem.setType(URI.create("https://estudalivre.local/problems/study-cycle-not-found"));
        return problem;
    }

    @ExceptionHandler(StudyCycleNotActivatableException.class)
    ProblemDetail cycleNotActivatable(StudyCycleNotActivatableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Ciclo ainda não pode ser ativado");
        problem.setType(URI.create("https://estudalivre.local/problems/study-cycle-not-activatable"));
        return problem;
    }

    @ExceptionHandler(ActiveStudyCycleCannotBeEmptyException.class)
    ProblemDetail activeCycleCannotBeEmpty(ActiveStudyCycleCannotBeEmptyException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Ciclo ativo precisa de atividades");
        problem.setType(URI.create("https://estudalivre.local/problems/active-study-cycle-empty"));
        return problem;
    }

    @ExceptionHandler(StudyCycleSwitchDecisionRequiredException.class)
    ProblemDetail switchDecisionRequired(StudyCycleSwitchDecisionRequiredException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Escolha como trocar de ciclo");
        problem.setType(URI.create("https://estudalivre.local/problems/study-cycle-switch-decision-required"));
        return problem;
    }

    @ExceptionHandler(SubjectNotFoundException.class)
    ProblemDetail subjectNotFound(SubjectNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Matéria não encontrada");
        problem.setType(URI.create("https://estudalivre.local/problems/subject-not-found"));
        return problem;
    }
}
