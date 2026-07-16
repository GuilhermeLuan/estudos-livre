package br.com.estudalivre.identity.service;

import java.util.Map;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

@Service
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class IdentitySessionService {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public IdentitySessionService(FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public void invalidateOtherSessions(String principalName, String currentSessionId) {
        sessionsFor(principalName).keySet().stream()
                .filter(sessionId -> !sessionId.equals(currentSessionId))
                .forEach(sessionRepository::deleteById);
    }

    public void invalidateAllSessions(String principalName) {
        sessionsFor(principalName).keySet().forEach(sessionRepository::deleteById);
    }

    private Map<String, ? extends Session> sessionsFor(String principalName) {
        return sessionRepository.findByPrincipalName(principalName);
    }
}
