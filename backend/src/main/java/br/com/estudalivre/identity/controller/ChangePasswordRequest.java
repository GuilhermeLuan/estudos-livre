package br.com.estudalivre.identity.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Informe a senha atual.") String currentPassword,
        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 12, max = 128, message = "A nova senha deve ter entre 12 e 128 caracteres.")
        String newPassword) {
}
