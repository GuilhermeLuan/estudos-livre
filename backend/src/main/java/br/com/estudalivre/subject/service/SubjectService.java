package br.com.estudalivre.subject.service;

import br.com.estudalivre.subject.dto.SubjectNameRequest;
import br.com.estudalivre.subject.dto.SubjectResponse;
import br.com.estudalivre.subject.model.Subject;
import br.com.estudalivre.subject.model.SubjectStatus;
import br.com.estudalivre.subject.repository.SubjectRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @Transactional
    public SubjectResponse create(UUID ownerId, SubjectNameRequest request) {
        UUID id = UUID.randomUUID();
        subjectRepository.create(id, ownerId, request.name());
        Subject subject = subjectRepository.findByIdAndOwnerId(id, ownerId).orElseThrow();
        return SubjectResponse.from(subject);
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> list(UUID ownerId, SubjectStatus status) {
        List<Subject> subjects = status == SubjectStatus.ACTIVE
                ? subjectRepository.findActiveByOwnerId(ownerId)
                : subjectRepository.findArchivedByOwnerId(ownerId);
        return subjects.stream()
                .map(SubjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubjectResponse get(UUID ownerId, UUID subjectId) {
        return SubjectResponse.from(findOwned(subjectId, ownerId));
    }

    @Transactional(readOnly = true)
    public SubjectResponse getActive(UUID ownerId, UUID subjectId) {
        Subject subject = findOwned(subjectId, ownerId);
        if (subject.archived()) {
            throw new SubjectNotFoundException();
        }
        return SubjectResponse.from(subject);
    }

    @Transactional
    public SubjectResponse update(UUID ownerId, UUID subjectId, SubjectNameRequest request) {
        if (subjectRepository.updateName(subjectId, ownerId, request.name()) == 0) {
            throw new SubjectNotFoundException();
        }
        return SubjectResponse.from(findOwned(subjectId, ownerId));
    }

    @Transactional
    public SubjectResponse archive(UUID ownerId, UUID subjectId) {
        if (subjectRepository.archive(subjectId, ownerId) == 0) {
            throw new SubjectNotFoundException();
        }
        return SubjectResponse.from(findOwned(subjectId, ownerId));
    }

    @Transactional
    public SubjectResponse restore(UUID ownerId, UUID subjectId) {
        if (subjectRepository.restore(subjectId, ownerId) == 0) {
            throw new SubjectNotFoundException();
        }
        return SubjectResponse.from(findOwned(subjectId, ownerId));
    }

    private Subject findOwned(UUID subjectId, UUID ownerId) {
        return subjectRepository.findByIdAndOwnerId(subjectId, ownerId)
                .orElseThrow(SubjectNotFoundException::new);
    }
}
