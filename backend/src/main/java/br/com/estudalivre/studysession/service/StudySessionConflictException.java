package br.com.estudalivre.studysession.service;

public class StudySessionConflictException extends RuntimeException {
    public StudySessionConflictException() {
        super("A sessão foi alterada ou não está concluída.");
    }
}
