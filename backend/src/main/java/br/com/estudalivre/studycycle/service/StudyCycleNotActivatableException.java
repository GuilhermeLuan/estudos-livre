package br.com.estudalivre.studycycle.service;

public class StudyCycleNotActivatableException extends RuntimeException {

    public StudyCycleNotActivatableException() {
        super("Adicione pelo menos uma etapa antes de ativar o ciclo.");
    }
}
