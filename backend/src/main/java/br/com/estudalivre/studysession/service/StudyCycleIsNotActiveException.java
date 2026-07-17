package br.com.estudalivre.studysession.service;

public class StudyCycleIsNotActiveException extends RuntimeException {
    public StudyCycleIsNotActiveException() {
        super("Ative o ciclo antes de iniciar uma sessão por sua etapa atual.");
    }
}
