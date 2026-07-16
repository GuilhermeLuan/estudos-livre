package br.com.estudalivre.studycycle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStudyCycleRequest(
        @NotBlank(message = "Informe o nome do ciclo.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String name) {

    public CreateStudyCycleRequest {
        name = name == null ? null : name.strip().replaceAll("\\s+", " ");
    }
}
