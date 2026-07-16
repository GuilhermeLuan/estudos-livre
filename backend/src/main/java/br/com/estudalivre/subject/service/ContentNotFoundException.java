package br.com.estudalivre.subject.service;

public class ContentNotFoundException extends RuntimeException {

    public ContentNotFoundException() {
        super("O conteúdo informado não existe.");
    }
}
