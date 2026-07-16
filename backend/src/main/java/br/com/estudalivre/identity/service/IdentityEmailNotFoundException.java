package br.com.estudalivre.identity.service;

public class IdentityEmailNotFoundException extends RuntimeException {

    public IdentityEmailNotFoundException() {
        super("Não existe uma conta com este e-mail.");
    }
}
