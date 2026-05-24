package geumjeongyahak.domain.teacher_application.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.teacher_application.enums.TeacherApplicationStatus;
import geumjeongyahak.domain.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "teacher_applications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeacherApplication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Column(name = "applicant_name", nullable = false, length = 50)
    private String applicantName;

    @Column(name = "applicant_phone_number", nullable = false, length = 20)
    private String applicantPhoneNumber;

    @Column(name = "applicant_email", nullable = false, length = 100)
    private String applicantEmail;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "education_and_major", nullable = false, length = 255)
    private String educationAndMajor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_subject_id", nullable = false)
    private Subject preferredSubject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String motivation;

    @Column(name = "desired_teacher_image", columnDefinition = "TEXT", nullable = false)
    private String desiredTeacherImage;

    @Column(name = "meaning_of_sharing", columnDefinition = "TEXT", nullable = false)
    private String meaningOfSharing;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeacherApplicationStatus status;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    public TeacherApplication(
        User applicant,
        String applicantName,
        String applicantPhoneNumber,
        String applicantEmail,
        LocalDate birthDate,
        String address,
        String educationAndMajor,
        Subject preferredSubject,
        String motivation,
        String desiredTeacherImage,
        String meaningOfSharing
    ) {
        this.applicant = applicant;
        this.applicantName = applicantName;
        this.applicantPhoneNumber = applicantPhoneNumber;
        this.applicantEmail = applicantEmail;
        this.birthDate = birthDate;
        this.address = address;
        this.educationAndMajor = educationAndMajor;
        this.preferredSubject = preferredSubject;
        this.motivation = motivation;
        this.desiredTeacherImage = desiredTeacherImage;
        this.meaningOfSharing = meaningOfSharing;
        this.status = TeacherApplicationStatus.PENDING;
    }

    public void update(
        String applicantPhoneNumber,
        String applicantEmail,
        LocalDate birthDate,
        String address,
        String educationAndMajor,
        Subject preferredSubject,
        String motivation,
        String desiredTeacherImage,
        String meaningOfSharing
    ) {
        this.applicantPhoneNumber = applicantPhoneNumber;
        this.applicantEmail = applicantEmail;
        this.birthDate = birthDate;
        this.address = address;
        this.educationAndMajor = educationAndMajor;
        this.preferredSubject = preferredSubject;
        this.motivation = motivation;
        this.desiredTeacherImage = desiredTeacherImage;
        this.meaningOfSharing = meaningOfSharing;
    }

    public void approve(User reviewer, String reviewNote) {
        this.status = TeacherApplicationStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.reviewNote = reviewNote;
    }

    public void reject(User reviewer, String reviewNote) {
        this.status = TeacherApplicationStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.reviewNote = reviewNote;
    }

    public void cancel() {
        this.status = TeacherApplicationStatus.CANCELLED;
    }
}
