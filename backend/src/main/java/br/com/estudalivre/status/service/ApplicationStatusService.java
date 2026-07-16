package br.com.estudalivre.status.service;

import br.com.estudalivre.status.dto.ApplicationStatusResponse;
import br.com.estudalivre.status.repository.ApplicationStatusRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationStatusService {

    private final ApplicationStatusRepository repository;
    private final String applicationVersion;

    public ApplicationStatusService(
            ApplicationStatusRepository repository,
            @Value("${info.app.version:dev}") String applicationVersion
    ) {
        this.repository = repository;
        this.applicationVersion = applicationVersion;
    }

    @Transactional(readOnly = true)
    public ApplicationStatusResponse getStatus() {
        return new ApplicationStatusResponse(
                "UP",
                "UP",
                repository.findSchemaVersion(),
                applicationVersion
        );
    }
}
