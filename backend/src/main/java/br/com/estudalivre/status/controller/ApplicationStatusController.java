package br.com.estudalivre.status.controller;

import br.com.estudalivre.status.dto.ApplicationStatusResponse;
import br.com.estudalivre.status.service.ApplicationStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
public class ApplicationStatusController {

    private final ApplicationStatusService applicationStatusService;

    public ApplicationStatusController(ApplicationStatusService applicationStatusService) {
        this.applicationStatusService = applicationStatusService;
    }

    @GetMapping
    public ApplicationStatusResponse getStatus() {
        return applicationStatusService.getStatus();
    }
}
