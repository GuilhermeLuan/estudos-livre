package br.com.estudalivre.identity.service;

import br.com.estudalivre.identity.controller.BootstrapAccountRequest;
import br.com.estudalivre.identity.repository.IdentityUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityBootstrapService {

    private final IdentityUserRepository identityUserRepository;
    private final IdentityAccountService identityAccountService;

    public IdentityBootstrapService(
            IdentityUserRepository identityUserRepository,
            IdentityAccountService identityAccountService) {
        this.identityUserRepository = identityUserRepository;
        this.identityAccountService = identityAccountService;
    }

    @Transactional
    public void createFirstAccount(BootstrapAccountRequest request) {
        identityUserRepository.lockBootstrap();
        if (identityUserRepository.existsAny()) {
            throw new BootstrapAlreadyCompletedException();
        }

        identityAccountService.create(request);
    }
}
