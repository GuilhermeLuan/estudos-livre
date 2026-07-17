package br.com.estudalivre.studysession.service;

public class InvalidStudySessionTransitionException extends RuntimeException {
    public InvalidStudySessionTransitionException(String message) {
        super(message);
    }
}
