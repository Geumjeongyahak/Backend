package geumjeongyahak.unit.file;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import geumjeongyahak.domain.file.config.FileCleanupProperties;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.file.service.FileCleanupScheduler;
import geumjeongyahak.domain.file.service.StorageService;
import geumjeongyahak.domain.post.repository.PostAttachmentRepository;
import geumjeongyahak.domain.post.repository.PostFileRepository;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestPaymentTransactionRepository;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestProposalReceiptRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class FileCleanupSchedulerTest {

    @Mock
    private FileRepository fileRepository;
    @Mock
    private PostFileRepository postFileRepository;
    @Mock
    private PostAttachmentRepository postAttachmentRepository;
    @Mock
    private PurchaseRequestPaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private PurchaseRequestProposalReceiptRepository proposalReceiptRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private FileCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        FileCleanupProperties properties = new FileCleanupProperties();
        properties.setChunkSize(1);
        scheduler = new FileCleanupScheduler(
            fileRepository,
            postFileRepository,
            postAttachmentRepository,
            paymentTransactionRepository,
            proposalReceiptRepository,
            storageService,
            properties,
            new TransactionTemplate(transactionManager)
        );
        given(fileRepository.findUnlinkedPurchaseItemFilesBefore(anyString(), any())).willReturn(List.of());
    }

    @Test
    void storageDeleteFailure_keepsRowAndContinuesWithNextChunk() {
        File failed = gcsFile("failed.pdf");
        File ok = gcsFile("ok.pdf");
        given(fileRepository.findByIsDeletedTrueAndDeletedAtBefore(any())).willReturn(List.of(failed, ok));
        given(storageService.delete("failed.pdf")).willReturn(false);
        given(storageService.delete("ok.pdf")).willReturn(true);

        scheduler.cleanupDeletedFiles();

        then(fileRepository).should(never()).deleteById(failed.getId());
        then(postFileRepository).should(never()).deleteByFileId(failed.getId());
        then(fileRepository).should().deleteById(ok.getId());
    }

    @Test
    void dbFailureInOneChunk_rollsBackOnlyThatChunk() {
        File broken = gcsFile("broken.pdf");
        File ok = gcsFile("ok.pdf");
        given(fileRepository.findByIsDeletedTrueAndDeletedAtBefore(any())).willReturn(List.of(broken, ok));
        given(storageService.delete(anyString())).willReturn(true);
        willThrow(new IllegalStateException("db down")).given(postFileRepository).deleteByFileId(broken.getId());

        scheduler.cleanupDeletedFiles();

        then(fileRepository).should(never()).deleteById(broken.getId());
        then(fileRepository).should().deleteById(ok.getId());
        then(transactionManager).should(times(1)).rollback(any());
        // 미연동 파일 표시 1번 + 성공한 청크 1번
        then(transactionManager).should(times(2)).commit(any());
    }

    @Test
    void storageDelete_runsOutsideTransaction() {
        File file = gcsFile("a.pdf");
        given(fileRepository.findByIsDeletedTrueAndDeletedAtBefore(any())).willReturn(List.of(file));
        given(storageService.delete("a.pdf")).willAnswer(invocation -> {
            // GCS 삭제 시점에는 청크 트랜잭션이 아직 열리지 않았다 (미연동 파일 표시 트랜잭션 1개만)
            then(transactionManager).should(times(1)).getTransaction(any());
            return true;
        });

        scheduler.cleanupDeletedFiles();

        then(transactionManager).should(times(2)).getTransaction(any());
    }

    private File gcsFile(String storageKey) {
        File file = File.builder()
            .storageKey(storageKey)
            .bucket("bucket")
            .contentType("application/pdf")
            .ext("pdf")
            .build();
        ReflectionTestUtils.setField(file, "id", UUID.randomUUID());
        return file;
    }
}
