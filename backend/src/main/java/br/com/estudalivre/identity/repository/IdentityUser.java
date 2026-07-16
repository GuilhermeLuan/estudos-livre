package br.com.estudalivre.identity.repository;

import java.util.UUID;

public record IdentityUser(UUID id, String email, String passwordHash, String timeZone) {
}
