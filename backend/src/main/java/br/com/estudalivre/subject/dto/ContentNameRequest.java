package br.com.estudalivre.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContentNameRequest(
        @NotBlank(message = "Informe o nome do conteúdo.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String name) {

    public ContentNameRequest {
        name = name == null ? null : name.strip().replaceAll("\\s+", " ");
    }
}
