package geumjeongyahak.domain.file.service;

import geumjeongyahak.domain.file.config.FileCleanupProperties;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.post.repository.PostAttachmentRepository;
import geumjeongyahak.domain.post.repository.PostFileRepository;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestPaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final StorageService storageService;
    private final FileCleanupProperties fileCleanupProperties;

    @Scheduled(cron = "${app.file.cleanup.cron}")
    @Transactional
    public void cleanupDeletedFiles() {
        markStaleUnlinkedPurchaseItemFiles();

        LocalDateTime threshold = LocalDateTime.now().minusDays(fileCleanupProperties.getRetentionDays());
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

    private int processChunk(List<File> files) {
        int count = 0;
        for (File file : files) {
            if (!deleteStorageObject(file)) {
                continue;
            }
            postFileRepository.deleteByFileId(file.getId());
            postAttachmentRepository.deleteByFileId(file.getId());
            purchaseRequestPaymentTransactionRepository.clearReceiptFileByFileId(file.getId());
            fileRepository.delete(file);
            count++;
        }
        return count;
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
