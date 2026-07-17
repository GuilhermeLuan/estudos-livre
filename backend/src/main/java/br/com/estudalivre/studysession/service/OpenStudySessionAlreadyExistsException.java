package br.com.estudalivre.studysession.service;

public class OpenStudySessionAlreadyExistsException extends RuntimeException {
    public OpenStudySessionAlreadyExistsException() {
        super("Já existe um cronômetro ativo ou pausado para esta conta.");
    }
}
