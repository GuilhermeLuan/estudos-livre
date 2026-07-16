package br.com.estudalivre.subject.service;

public class DuplicateContentNameException extends RuntimeException {

    public DuplicateContentNameException() {
        super("Já existe um conteúdo ativo com esse nome nesta matéria.");
    }
}
