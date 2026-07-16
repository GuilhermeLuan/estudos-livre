package br.com.estudalivre.subject.service;

import br.com.estudalivre.subject.dto.ContentNameRequest;
import br.com.estudalivre.subject.dto.ContentResponse;
import br.com.estudalivre.subject.model.Content;
import br.com.estudalivre.subject.model.SubjectStatus;
import br.com.estudalivre.subject.repository.ContentRepository;
import br.com.estudalivre.subject.repository.SubjectRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentService {

    private final ContentRepository contentRepository;
    private final SubjectRepository subjectRepository;

    public ContentService(ContentRepository contentRepository, SubjectRepository subjectRepository) {
        this.contentRepository = contentRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional
    public ContentResponse create(UUID ownerId, UUID subjectId, ContentNameRequest request) {
        requireOwnedSubject(subjectId, ownerId);
        UUID id = UUID.randomUUID();
        try {
            contentRepository.create(id, subjectId, request.name());
        } catch (DuplicateKeyException exception) {
            throw new DuplicateContentNameException();
        }
        return ContentResponse.from(findOwned(id, subjectId, ownerId));
    }

    @Transactional(readOnly = true)
    public List<ContentResponse> list(UUID ownerId, UUID subjectId, SubjectStatus status) {
        requireOwnedSubject(subjectId, ownerId);
        List<Content> contents = status == SubjectStatus.ACTIVE
                ? contentRepository.findActiveBySubjectIdAndOwnerId(subjectId, ownerId)
                : contentRepository.findArchivedBySubjectIdAndOwnerId(subjectId, ownerId);
        return contents.stream().map(ContentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ContentResponse get(UUID ownerId, UUID subjectId, UUID contentId) {
        return ContentResponse.from(findOwned(contentId, subjectId, ownerId));
    }

    @Transactional
    public ContentResponse update(
            UUID ownerId, UUID subjectId, UUID contentId, ContentNameRequest request) {
        try {
            if (contentRepository.updateName(contentId, subjectId, ownerId, request.name()) == 0) {
                throw new ContentNotFoundException();
            }
        } catch (DuplicateKeyException exception) {
            throw new DuplicateContentNameException();
        }
        return ContentResponse.from(findOwned(contentId, subjectId, ownerId));
    }

    @Transactional
    public ContentResponse archive(UUID ownerId, UUID subjectId, UUID contentId) {
        if (contentRepository.archive(contentId, subjectId, ownerId) == 0) {
            throw new ContentNotFoundException();
        }
        return ContentResponse.from(findOwned(contentId, subjectId, ownerId));
    }

    @Transactional
    public ContentResponse restore(UUID ownerId, UUID subjectId, UUID contentId) {
        try {
            if (contentRepository.restore(contentId, subjectId, ownerId) == 0) {
                throw new ContentNotFoundException();
            }
        } catch (DuplicateKeyException exception) {
            throw new DuplicateContentNameException();
        }
        return ContentResponse.from(findOwned(contentId, subjectId, ownerId));
    }

    private Content findOwned(UUID contentId, UUID subjectId, UUID ownerId) {
        return contentRepository.findByIdAndSubjectIdAndOwnerId(contentId, subjectId, ownerId)
                .orElseThrow(ContentNotFoundException::new);
    }

    private void requireOwnedSubject(UUID subjectId, UUID ownerId) {
        subjectRepository.findByIdAndOwnerId(subjectId, ownerId)
                .orElseThrow(SubjectNotFoundException::new);
    }
}
