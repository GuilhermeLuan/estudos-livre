package br.com.estudalivre.subject.service;

public class SubjectInUseException extends RuntimeException {

    public SubjectInUseException() {
        super("Esta matéria já foi usada em um ciclo, sessão de estudo ou revisão. Arquive-a para preservar o histórico.");
    }
}
