package br.com.estudalivre.subject.service;

public class SubjectNotFoundException extends RuntimeException {

    public SubjectNotFoundException() {
        super("A matéria informada não foi encontrada.");
    }
}
