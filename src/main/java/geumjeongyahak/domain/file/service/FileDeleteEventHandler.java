package geumjeongyahak.domain.file.service;

import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.post.event.PostDeletedEvent;
import geumjeongyahak.domain.post.event.PostImageDeleteRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDeleteEventHandler {

    private final FileRepository fileRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostDeleted(PostDeletedEvent event) {
        deleteFiles(event.getFileIds());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostImageDeleteRequested(PostImageDeleteRequestedEvent event) {
        deleteFiles(event.getFileIds());
    }

    private void deleteFiles(Set<UUID> fileIds) {
        if (fileIds.isEmpty()) {
            return;
        }

        fileRepository.findAllByIdInAndIsDeletedFalse(fileIds).forEach(file -> {
            file.delete();
            log.info("고아 파일 Soft Delete (fileId={})", file.getId());
        });
    }
}
