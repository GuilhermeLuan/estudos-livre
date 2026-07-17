package br.com.estudalivre.studycycle.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateSuggestedStudyCycleRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @Size(min = 1, max = 30) List<@NotNull @Valid SuggestedStudyCycleSubjectRequest> subjects) {

    public CreateSuggestedStudyCycleRequest {
        if (name != null) {
            name = name.trim();
        }
        subjects = subjects == null ? null : List.copyOf(subjects);
    }
}
