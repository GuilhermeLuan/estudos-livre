package br.com.estudalivre.identity.service;

public class PasswordResetTokenInvalidException extends RuntimeException {

    public PasswordResetTokenInvalidException() {
        super("Este link de redefinição é inválido ou expirou.");
    }
}
