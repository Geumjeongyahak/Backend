package geumjeongyahak.domain.file.service;

import geumjeongyahak.domain.file.config.FileCleanupProperties;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.post.repository.PostAttachmentRepository;
import geumjeongyahak.domain.post.repository.PostFileRepository;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestPaymentTransactionRepository;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestProposalReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupScheduler {

    private static final String PURCHASE_ITEM_STORAGE_PREFIX = "documents/purchase-items/";

    private final FileRepository fileRepository;
    private final PostFileRepository postFileRepository;
    private final PostAttachmentRepository postAttachmentRepository;
    private final PurchaseRequestPaymentTransactionRepository purchaseRequestPaymentTransactionRepository;
    private final PurchaseRequestProposalReceiptRepository purchaseRequestProposalReceiptRepository;
    private final StorageService storageService;
    private final FileCleanupProperties fileCleanupProperties;
    private final TransactionTemplate transactionTemplate;

    // ponytail: 여러 서버 동시 실행을 막는 락(ShedLock)이 없다. 앱 서버가 1대라서다. 2대 이상이 되면 ShedLock 을 붙인다.
    @Scheduled(cron = "${app.file.cleanup.cron}")
    public void cleanupDeletedFiles() {
        transactionTemplate.executeWithoutResult(status -> markStaleUnlinkedPurchaseItemFiles());

        LocalDateTime threshold = LocalDateTime.now().minusDays(fileCleanupProperties.getRetentionDays());
        // 리포지토리 조회 메서드는 자체 읽기 트랜잭션으로 짧게 끝난다
        List<File> candidates = fileRepository.findByIsDeletedTrueAndDeletedAtBefore(threshold);

        if (candidates.isEmpty()) {
            return;
        }

        int chunkSize = fileCleanupProperties.getChunkSize();
        int processed = 0;

        for (int i = 0; i < candidates.size(); i += chunkSize) {
            List<File> chunk = candidates.subList(i, Math.min(i + chunkSize, candidates.size()));
            processed += processChunk(chunk);
        }

        log.info("파일 Hard Delete 스케줄러 실행 완료 (count={})", processed);
    }

    private void markStaleUnlinkedPurchaseItemFiles() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(fileCleanupProperties.getTemporaryRetentionHours());
        List<File> candidates = fileRepository.findUnlinkedPurchaseItemFilesBefore(PURCHASE_ITEM_STORAGE_PREFIX, threshold);

        candidates.forEach(File::delete);

        if (!candidates.isEmpty()) {
            log.info("미연동 구매 영수증 파일 Soft Delete 완료 (count={})", candidates.size());
        }
    }

    // 저장소 삭제는 트랜잭션 밖에서 먼저 한다. DB 정리가 실패해도 다음 실행에서
    // 없는 객체 삭제가 성공(true)으로 통과하므로 다시 정리된다.
    private int processChunk(List<File> files) {
        List<File> storageDeleted = files.stream().filter(this::deleteStorageObject).toList();
        if (storageDeleted.isEmpty()) {
            return 0;
        }

        try {
            transactionTemplate.executeWithoutResult(status -> storageDeleted.forEach(this::deleteFileRows));
            return storageDeleted.size();
        } catch (RuntimeException e) {
            log.error("파일 DB 정리 실패, 다음 실행에서 다시 시도 (count={})", storageDeleted.size(), e);
            return 0;
        }
    }

    private void deleteFileRows(File file) {
        postFileRepository.deleteByFileId(file.getId());
        postAttachmentRepository.deleteByFileId(file.getId());
        purchaseRequestPaymentTransactionRepository.clearReceiptFileByFileId(file.getId());
        purchaseRequestProposalReceiptRepository.deleteAllByFileId(file.getId());
        fileRepository.deleteById(file.getId());
    }

    private boolean deleteStorageObject(File file) {
        if (file.isGoogleDriveFile()) {
            return true;
        }

        boolean gcsDeleted = storageService.delete(file.getStorageKey());
        if (!gcsDeleted) {
            log.warn("GCS 삭제 실패, DB 레코드 유지 (fileId={}, path={})", file.getId(), file.getStorageKey());
        }
        return gcsDeleted;
    }
}
