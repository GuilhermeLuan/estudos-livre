package br.com.estudalivre.status.dto;

public record ApplicationStatusResponse(
        String status,
        String database,
        String schemaVersion,
        String version
) {
}
