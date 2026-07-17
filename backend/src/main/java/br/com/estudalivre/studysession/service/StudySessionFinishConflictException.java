package br.com.estudalivre.studysession.service;

public class StudySessionFinishConflictException extends RuntimeException {

    public StudySessionFinishConflictException() {
        super("A sessão foi finalizada ou alterada depois que este formulário foi aberto.");
    }
}
