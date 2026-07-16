package br.com.estudalivre.studycycle.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StudyCycleStageRequest(
        @NotNull(message = "Selecione uma matéria.")
        UUID subjectId,

        @NotNull(message = "Informe a duração da etapa.")
        @Min(value = 5, message = "A duração deve ser de pelo menos 5 minutos.")
        Integer targetMinutes) {
}
