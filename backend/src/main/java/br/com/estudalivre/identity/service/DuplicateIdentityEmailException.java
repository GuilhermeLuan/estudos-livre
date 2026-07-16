package br.com.estudalivre.identity.service;

public class DuplicateIdentityEmailException extends RuntimeException {

    public DuplicateIdentityEmailException() {
        super("Já existe uma conta com este e-mail.");
    }
}
