package br.com.estudalivre.subject.controller;

import br.com.estudalivre.subject.service.DuplicateContentNameException;
import br.com.estudalivre.subject.service.ContentNotFoundException;
import br.com.estudalivre.subject.service.SubjectNotFoundException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ContentController.class)
public class ContentExceptionHandler {

    @ExceptionHandler(DuplicateContentNameException.class)
    ProblemDetail duplicateContentName(DuplicateContentNameException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Conteúdo duplicado");
        problem.setType(URI.create("https://estudalivre.local/problems/duplicate-content-name"));
        return problem;
    }

    @ExceptionHandler(SubjectNotFoundException.class)
    ProblemDetail subjectNotFound(SubjectNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Matéria não encontrada");
        problem.setType(URI.create("https://estudalivre.local/problems/subject-not-found"));
        return problem;
    }

    @ExceptionHandler(ContentNotFoundException.class)
    ProblemDetail contentNotFound(ContentNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Conteúdo não encontrado");
        problem.setType(URI.create("https://estudalivre.local/problems/content-not-found"));
        return problem;
    }
}
