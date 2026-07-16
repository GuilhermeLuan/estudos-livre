package br.com.estudalivre.identity.service;

public class RegistrationClosedException extends RuntimeException {

    public RegistrationClosedException() {
        super("O cadastro público está desabilitado nesta instalação.");
    }
}
