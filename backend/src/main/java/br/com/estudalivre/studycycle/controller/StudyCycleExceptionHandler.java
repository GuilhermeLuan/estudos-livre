package br.com.estudalivre.studycycle.controller;

import br.com.estudalivre.studycycle.service.StudyCycleNotFoundException;
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

    @ExceptionHandler(SubjectNotFoundException.class)
    ProblemDetail subjectNotFound(SubjectNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Matéria não encontrada");
        problem.setType(URI.create("https://estudalivre.local/problems/subject-not-found"));
        return problem;
    }
}
