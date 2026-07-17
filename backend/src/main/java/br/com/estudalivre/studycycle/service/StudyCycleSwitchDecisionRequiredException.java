package br.com.estudalivre.studycycle.service;

public class StudyCycleSwitchDecisionRequiredException extends RuntimeException {

    public StudyCycleSwitchDecisionRequiredException() {
        super("Escolha se deseja pausar ou encerrar a volta do ciclo ativo.");
    }
}
