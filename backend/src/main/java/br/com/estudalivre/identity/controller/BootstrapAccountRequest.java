package br.com.estudalivre.identity.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record BootstrapAccountRequest(
        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 320, message = "O e-mail deve ter no máximo 320 caracteres.") String email,
        @NotBlank(message = "Informe a senha.")
        @Size(min = 12, max = 128, message = "A senha deve ter entre 12 e 128 caracteres.") String password,
        @NotBlank(message = "Informe o fuso horário.")
        @Size(max = 255, message = "O fuso horário deve ter no máximo 255 caracteres.") String timeZone) {

    public BootstrapAccountRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        timeZone = timeZone == null ? null : timeZone.strip();
    }
}
