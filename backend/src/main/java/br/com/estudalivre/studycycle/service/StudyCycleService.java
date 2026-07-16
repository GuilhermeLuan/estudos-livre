package br.com.estudalivre.studycycle.service;

import br.com.estudalivre.studycycle.dto.CreateStudyCycleRequest;
import br.com.estudalivre.studycycle.dto.StudyCycleResponse;
import br.com.estudalivre.studycycle.dto.UpdateStudyCycleRequest;
import br.com.estudalivre.studycycle.model.StudyCycle;
import br.com.estudalivre.studycycle.repository.StudyCycleRepository;
import br.com.estudalivre.subject.service.SubjectService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyCycleService {

    private final StudyCycleRepository studyCycleRepository;
    private final SubjectService subjectService;

    public StudyCycleService(StudyCycleRepository studyCycleRepository, SubjectService subjectService) {
        this.studyCycleRepository = studyCycleRepository;
        this.subjectService = subjectService;
    }

    @Transactional
    public StudyCycleResponse create(UUID ownerId, CreateStudyCycleRequest request) {
        UUID cycleId = UUID.randomUUID();
        studyCycleRepository.create(cycleId, ownerId, request.name());
        StudyCycle cycle = studyCycleRepository.findByIdAndOwnerId(cycleId, ownerId).orElseThrow();
        return toResponse(cycle);
    }

    @Transactional(readOnly = true)
    public List<StudyCycleResponse> list(UUID ownerId) {
        return studyCycleRepository.findByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudyCycleResponse get(UUID ownerId, UUID cycleId) {
        return toResponse(findOwned(cycleId, ownerId));
    }

    @Transactional
    public StudyCycleResponse update(UUID ownerId, UUID cycleId, UpdateStudyCycleRequest request) {
        findOwned(cycleId, ownerId);
        if (request.stages().stream().anyMatch(stage -> stage.targetMinutes() % 5 != 0)) {
            throw new IllegalArgumentException("A duração de cada etapa deve ser múltipla de 5 minutos.");
        }
        request.stages().forEach(stage -> subjectService.getActive(ownerId, stage.subjectId()));

        if (studyCycleRepository.updateDraft(cycleId, ownerId, request.name()) == 0) {
            throw new StudyCycleNotFoundException();
        }
        studyCycleRepository.deleteStages(cycleId);
        for (int index = 0; index < request.stages().size(); index++) {
            var stage = request.stages().get(index);
            studyCycleRepository.createStage(
                    UUID.randomUUID(), cycleId, stage.subjectId(), index + 1, stage.targetMinutes());
        }
        return toResponse(findOwned(cycleId, ownerId));
    }

    @Transactional
    public StudyCycleResponse activate(UUID ownerId, UUID cycleId) {
        StudyCycle cycle = findOwned(cycleId, ownerId);
        if (studyCycleRepository.findStages(cycle.id()).isEmpty()) {
            throw new StudyCycleNotActivatableException();
        }
        studyCycleRepository.lockOwner(ownerId);
        if (studyCycleRepository.findCurrentRun(cycleId).isEmpty()) {
            studyCycleRepository.createRun(UUID.randomUUID(), cycleId);
        }
        studyCycleRepository.deactivateActiveCycles(ownerId, cycleId);
        studyCycleRepository.activate(cycleId, ownerId);
        return toResponse(findOwned(cycleId, ownerId));
    }

    private StudyCycle findOwned(UUID cycleId, UUID ownerId) {
        return studyCycleRepository.findByIdAndOwnerId(cycleId, ownerId)
                .orElseThrow(StudyCycleNotFoundException::new);
    }

    private StudyCycleResponse toResponse(StudyCycle cycle) {
        return StudyCycleResponse.from(
                cycle,
                studyCycleRepository.findStages(cycle.id()),
                studyCycleRepository.findCurrentRun(cycle.id()).orElse(null));
    }
}
