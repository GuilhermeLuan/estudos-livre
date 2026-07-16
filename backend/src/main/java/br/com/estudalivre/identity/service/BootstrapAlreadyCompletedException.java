package br.com.estudalivre.identity.service;

public class BootstrapAlreadyCompletedException extends RuntimeException {

    public BootstrapAlreadyCompletedException() {
        super("A primeira conta desta instalação já foi criada.");
    }
}
