package br.com.estudalivre.identity.service;

public class CurrentPasswordIncorrectException extends RuntimeException {

    public CurrentPasswordIncorrectException() {
        super("A senha atual não confere.");
    }
}
