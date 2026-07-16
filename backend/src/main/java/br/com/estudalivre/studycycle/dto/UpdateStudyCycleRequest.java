package br.com.estudalivre.studycycle.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record UpdateStudyCycleRequest(
        @NotBlank(message = "Informe o nome do ciclo.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String name,

        @NotNull(message = "Informe as etapas do ciclo.")
        List<@NotNull(message = "Informe os dados da etapa.") @Valid StudyCycleStageRequest> stages) {

    public UpdateStudyCycleRequest {
        name = name == null ? null : name.strip().replaceAll("\\s+", " ");
        stages = stages == null ? null : Collections.unmodifiableList(new ArrayList<>(stages));
    }
}
