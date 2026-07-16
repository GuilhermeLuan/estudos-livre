package br.com.estudalivre.subject.controller;

import br.com.estudalivre.subject.service.SubjectNotFoundException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = SubjectController.class)
public class SubjectExceptionHandler {

    @ExceptionHandler(SubjectNotFoundException.class)
    ProblemDetail subjectNotFound(SubjectNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Matéria não encontrada");
        problem.setType(URI.create("https://estudalivre.local/problems/subject-not-found"));
        return problem;
    }
}
