package br.com.estudalivre.studycycle.service;

public class ActiveStudyCycleCannotBeEmptyException extends RuntimeException {

    public ActiveStudyCycleCannotBeEmptyException() {
        super("Adicione ao menos uma atividade antes de salvar o ciclo ativo.");
    }
}
