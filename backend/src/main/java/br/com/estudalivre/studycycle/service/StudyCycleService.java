package br.com.estudalivre.studycycle.service;

import br.com.estudalivre.studycycle.dto.CreateStudyCycleRequest;
import br.com.estudalivre.studycycle.dto.CreateSuggestedStudyCycleRequest;
import br.com.estudalivre.studycycle.dto.CycleSwitchAction;
import br.com.estudalivre.studycycle.dto.StudyCycleResponse;
import br.com.estudalivre.studycycle.dto.StudyCycleSuggestionResponse;
import br.com.estudalivre.studycycle.dto.UpdateStudyCycleRequest;
import br.com.estudalivre.studycycle.model.StudyCycle;
import br.com.estudalivre.studycycle.planner.SuggestedStudyCycleInput;
import br.com.estudalivre.studycycle.planner.SuggestedStudyCyclePlan;
import br.com.estudalivre.studycycle.planner.SuggestedStudyCyclePlanner;
import br.com.estudalivre.studycycle.planner.SuggestedStudyCycleSubject;
import br.com.estudalivre.studycycle.repository.StudyCycleRepository;
import br.com.estudalivre.subject.service.SubjectService;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyCycleService {

    private final StudyCycleRepository studyCycleRepository;
    private final SubjectService subjectService;
    private final SuggestedStudyCyclePlanner suggestedStudyCyclePlanner;

    public StudyCycleService(
            StudyCycleRepository studyCycleRepository,
            SubjectService subjectService,
            SuggestedStudyCyclePlanner suggestedStudyCyclePlanner) {
        this.studyCycleRepository = studyCycleRepository;
        this.subjectService = subjectService;
        this.suggestedStudyCyclePlanner = suggestedStudyCyclePlanner;
    }

    @Transactional
    public StudyCycleResponse createSuggested(UUID ownerId, CreateSuggestedStudyCycleRequest request) {
        var plan = planSuggestion(ownerId, request);
        UUID cycleId = UUID.randomUUID();
        studyCycleRepository.createSuggested(cycleId, ownerId, request.name());
        storeSuggestion(cycleId, plan);
        return toResponse(findOwned(cycleId, ownerId));
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
        StudyCycle cycle = findOwned(cycleId, ownerId);
        if (cycle.status().equals("ACTIVE") && request.stages().isEmpty()) {
            throw new ActiveStudyCycleCannotBeEmptyException();
        }
        if (request.stages().stream().anyMatch(stage -> stage.targetMinutes() % 5 != 0)) {
            throw new IllegalArgumentException("A duração de cada etapa deve ser múltipla de 5 minutos.");
        }
        request.stages().forEach(stage -> subjectService.getActive(ownerId, stage.subjectId()));

        if (studyCycleRepository.update(cycleId, ownerId, request.name()) == 0) {
            throw new StudyCycleNotFoundException();
        }
        studyCycleRepository.deleteSuggestion(cycleId);
        studyCycleRepository.deleteStages(cycleId);
        for (int index = 0; index < request.stages().size(); index++) {
            var stage = request.stages().get(index);
            studyCycleRepository.createStage(
                    UUID.randomUUID(), cycleId, stage.subjectId(), index + 1, stage.targetMinutes());
        }
        return toResponse(findOwned(cycleId, ownerId));
    }

    @Transactional
    public StudyCycleResponse regenerateSuggestion(
            UUID ownerId,
            UUID cycleId,
            CreateSuggestedStudyCycleRequest request) {
        findOwned(cycleId, ownerId);
        var plan = planSuggestion(ownerId, request);

        if (studyCycleRepository.updateSuggested(cycleId, ownerId, request.name()) == 0) {
            throw new StudyCycleNotFoundException();
        }
        studyCycleRepository.deleteSuggestion(cycleId);
        studyCycleRepository.deleteStages(cycleId);
        storeSuggestion(cycleId, plan);
        return toResponse(findOwned(cycleId, ownerId));
    }

    private SuggestedStudyCyclePlan planSuggestion(
            UUID ownerId,
            CreateSuggestedStudyCycleRequest request) {
        var seenSubjectIds = new HashSet<UUID>();
        List<SuggestedStudyCycleInput> inputs = request.subjects().stream()
                .map(subject -> {
                    if (!seenSubjectIds.add(subject.subjectId())) {
                        throw new IllegalArgumentException("Cada matéria deve aparecer apenas uma vez na sugestão.");
                    }
                    var ownedSubject = subjectService.getActive(ownerId, subject.subjectId());
                    return new SuggestedStudyCycleInput(
                            subject.subjectId(),
                            ownedSubject.name(),
                            subject.questionCount(),
                            subject.weight(),
                            subject.difficulty());
                })
                .toList();
        return suggestedStudyCyclePlanner.generate(inputs);
    }

    private void storeSuggestion(UUID cycleId, SuggestedStudyCyclePlan plan) {
        for (int index = 0; index < plan.subjects().size(); index++) {
            studyCycleRepository.createSuggestionSubject(cycleId, index + 1, plan.subjects().get(index));
        }
        for (int index = 0; index < plan.stages().size(); index++) {
            var stage = plan.stages().get(index);
            studyCycleRepository.createStage(
                    UUID.randomUUID(), cycleId, stage.subjectId(), index + 1, stage.targetMinutes());
        }
    }

    @Transactional
    public StudyCycleResponse activate(UUID ownerId, UUID cycleId, CycleSwitchAction currentRunAction) {
        StudyCycle cycle = findOwned(cycleId, ownerId);
        if (studyCycleRepository.findStages(cycle.id()).isEmpty()) {
            throw new StudyCycleNotActivatableException();
        }
        studyCycleRepository.lockOwner(ownerId);
        var activeCycle = studyCycleRepository.findActiveByOwnerId(ownerId)
                .filter(current -> !current.id().equals(cycleId));
        if (activeCycle.isPresent() && currentRunAction == null) {
            throw new StudyCycleSwitchDecisionRequiredException();
        }
        if (activeCycle.isPresent() && currentRunAction == CycleSwitchAction.PAUSE) {
            studyCycleRepository.pauseCurrentRun(activeCycle.orElseThrow().id());
        }
        if (activeCycle.isPresent() && currentRunAction == CycleSwitchAction.ABANDON) {
            studyCycleRepository.abandonCurrentRun(activeCycle.orElseThrow().id());
        }

        var currentRun = studyCycleRepository.findCurrentRun(cycleId);
        if (currentRun.isEmpty()) {
            studyCycleRepository.createRun(UUID.randomUUID(), cycleId);
        } else if (currentRun.orElseThrow().status().equals("PAUSED")) {
            studyCycleRepository.resumeCurrentRun(cycleId);
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
        var stages = studyCycleRepository.findStages(cycle.id());
        var suggestionSubjects = cycle.mode().equals("SUGGESTED")
                ? studyCycleRepository.findSuggestionSubjects(cycle.id())
                : List.<SuggestedStudyCycleSubject>of();
        StudyCycleSuggestionResponse suggestion = suggestionSubjects.isEmpty()
                ? null
                : StudyCycleSuggestionResponse.from(
                        stages.stream().mapToInt(stage -> stage.targetMinutes()).sum(),
                        suggestionSubjects);
        return StudyCycleResponse.from(
                cycle,
                stages,
                studyCycleRepository.findCurrentRun(cycle.id()).orElse(null),
                suggestion);
    }
}
