package br.com.estudalivre.studysession.service;

public class StudySessionNotFoundException extends RuntimeException {
    public StudySessionNotFoundException() {
        super("A sessão de estudo não existe ou pertence a outra pessoa.");
    }
}
