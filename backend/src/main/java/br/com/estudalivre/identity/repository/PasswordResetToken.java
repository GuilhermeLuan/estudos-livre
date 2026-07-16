package br.com.estudalivre.identity.repository;

import java.util.UUID;

public record PasswordResetToken(String tokenHash, UUID userId, String email) {
}
