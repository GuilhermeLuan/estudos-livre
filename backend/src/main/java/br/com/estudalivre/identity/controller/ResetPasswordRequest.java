package br.com.estudalivre.identity.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Informe o token de redefinição.")
        @Size(max = 512, message = "O token de redefinição é inválido.") String token,
        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 12, max = 128, message = "A nova senha deve ter entre 12 e 128 caracteres.")
        String newPassword) {
}
