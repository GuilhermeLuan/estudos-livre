package br.com.estudalivre.studycycle.service;

public class StudyCycleNotFoundException extends RuntimeException {

    public StudyCycleNotFoundException() {
        super("O ciclo informado não foi encontrado.");
    }
}
