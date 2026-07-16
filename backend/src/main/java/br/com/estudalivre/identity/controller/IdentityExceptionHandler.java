package br.com.estudalivre.identity.controller;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import br.com.estudalivre.identity.service.BootstrapAlreadyCompletedException;
import br.com.estudalivre.identity.service.RegistrationClosedException;
import br.com.estudalivre.identity.service.DuplicateIdentityEmailException;
import br.com.estudalivre.identity.service.CurrentPasswordIncorrectException;
import br.com.estudalivre.identity.service.PasswordResetTokenInvalidException;
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

    @ExceptionHandler(RegistrationClosedException.class)
    ProblemDetail registrationClosed(RegistrationClosedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("Cadastro indisponível");
        return problem;
    }

    @ExceptionHandler(DuplicateIdentityEmailException.class)
    ProblemDetail duplicateEmail(DuplicateIdentityEmailException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("E-mail já cadastrado");
        return problem;
    }

    @ExceptionHandler(CurrentPasswordIncorrectException.class)
    ProblemDetail currentPasswordIncorrect(CurrentPasswordIncorrectException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Senha atual incorreta");
        return problem;
    }

    @ExceptionHandler(PasswordResetTokenInvalidException.class)
    ProblemDetail invalidPasswordResetToken(PasswordResetTokenInvalidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Link de redefinição inválido");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidRequest(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Dados inválidos");
        return problem;
    }
}
