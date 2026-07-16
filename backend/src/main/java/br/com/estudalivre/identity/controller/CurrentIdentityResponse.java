package br.com.estudalivre.identity.controller;

import java.util.UUID;

public record CurrentIdentityResponse(UUID id, String email, String timeZone) {
}
