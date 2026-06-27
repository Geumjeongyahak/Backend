package geumjeongyahak.unit.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.file.config.DriveUploadProperties;
import geumjeongyahak.domain.file.enums.DriveUploadTarget;

class DriveUploadPropertiesTest {

    @Test
    void folderIdFor_resolvesConfiguredTargets() {
        DriveUploadProperties properties = new DriveUploadProperties();
        properties.getFolderIds().setHandover("handover-folder");
        properties.getFolderIds().setExamMaterials("exam-folder");
        properties.getFolderIds().setDocumentForms("forms-folder");
        properties.getFolderIds().setMeetingRecords("meeting-folder");
        properties.getFolderIds().setBoard("board-folder");

        assertThat(properties.folderIdFor(DriveUploadTarget.HANDOVER)).isEqualTo("handover-folder");
        assertThat(properties.folderIdFor(DriveUploadTarget.EXAM_MATERIALS)).isEqualTo("exam-folder");
        assertThat(properties.folderIdFor(DriveUploadTarget.DOCUMENT_FORMS)).isEqualTo("forms-folder");
        assertThat(properties.folderIdFor(DriveUploadTarget.MEETING_RECORDS)).isEqualTo("meeting-folder");
        assertThat(properties.folderIdFor(DriveUploadTarget.BOARD)).isEqualTo("board-folder");
    }

    @Test
    void fromPath_resolvesMeetingRecords() {
        assertThat(DriveUploadTarget.fromPath("meetingRecords")).isEqualTo(DriveUploadTarget.MEETING_RECORDS);
    }

    @Test
    void folderIdFor_missingFolder_throwsBusinessException() {
        DriveUploadProperties properties = new DriveUploadProperties();

        assertThatThrownBy(() -> properties.folderIdFor(DriveUploadTarget.BOARD))
            .isInstanceOf(BusinessException.class);
    }
}
