package br.com.estudalivre.identity.controller;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import br.com.estudalivre.identity.service.BootstrapAlreadyCompletedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class IdentityExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidFields(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Revise os campos informados e tente novamente.");
        problem.setTitle("Dados inválidos");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(BootstrapAlreadyCompletedException.class)
    ProblemDetail bootstrapAlreadyCompleted(BootstrapAlreadyCompletedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Cadastro inicial indisponível");
        problem.setType(URI.create("https://estudalivre.local/problems/bootstrap-completed"));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidRequest(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Dados inválidos");
        return problem;
    }
}
