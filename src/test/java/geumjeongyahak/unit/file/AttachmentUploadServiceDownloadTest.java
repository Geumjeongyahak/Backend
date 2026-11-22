package geumjeongyahak.unit.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.common.validation.FileValidationSupport;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.file.service.AttachmentUploadService;
import geumjeongyahak.domain.file.service.StorageService;
import geumjeongyahak.domain.file.service.access.AttachmentReadPolicy;

class AttachmentUploadServiceDownloadTest {

    private static final UUID FILE_ID = UUID.randomUUID();

    private final FileRepository fileRepository = mock(FileRepository.class);
    private final StorageService storageService = mock(StorageService.class);
    private final AttachmentReadPolicy denying = mock(AttachmentReadPolicy.class);
    private final AttachmentReadPolicy allowing = mock(AttachmentReadPolicy.class);

    private final CustomUserDetails volunteer = user("ROLE_VOLUNTEER");

    @BeforeEach
    void setUp() {
        File file = File.builder()
            .storageKey("documents/attachments/a.pdf")
            .bucket("bucket")
            .originalName("a.pdf")
            .contentType("application/pdf")
            .fileSize(1L)
            .ext("pdf")
            .build();
        when(fileRepository.findByIdAndIsDeletedFalse(FILE_ID)).thenReturn(Optional.of(file));
        when(storageService.generateDownloadUrl(any(), any(Duration.class))).thenReturn("signed-url");
        when(allowing.canRead(FILE_ID, volunteer)).thenReturn(true);
    }

    @Test
    void anyPolicyAllows_downloadAllowed() {
        assertThat(service(denying, allowing).getDownloadUrl(FILE_ID, volunteer)).isEqualTo("signed-url");
    }

    @Test
    void allPoliciesDeny_accessDenied() {
        assertThatThrownBy(() -> service(denying).getDownloadUrl(FILE_ID, volunteer))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void noUser_allPoliciesDeny_accessDenied() {
        assertThatThrownBy(() -> service(denying).getDownloadUrl(FILE_ID, null))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void admin_skipsPolicies() {
        assertThat(service(denying).getDownloadUrl(FILE_ID, user("ROLE_ADMIN"))).isEqualTo("signed-url");
        verifyNoInteractions(denying);
    }

    private AttachmentUploadService service(AttachmentReadPolicy... policies) {
        return new AttachmentUploadService(
            fileRepository, storageService, mock(FileValidationSupport.class), List.of(policies));
    }

    private static CustomUserDetails user(String role) {
        return new CustomUserDetails(1L, 10L, "u@test.com", null, null, List.of(new SimpleGrantedAuthority(role)));
    }
}
