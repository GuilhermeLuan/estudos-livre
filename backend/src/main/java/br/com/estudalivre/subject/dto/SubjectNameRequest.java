package br.com.estudalivre.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubjectNameRequest(
        @NotBlank(message = "Informe o nome da matéria.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String name) {

    public SubjectNameRequest {
        name = name == null ? null : name.strip();
    }
}
