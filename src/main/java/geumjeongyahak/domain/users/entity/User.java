package geumjeongyahak.domain.users.entity;

import geumjeongyahak.domain.auth.entity.UserCredential;
import geumjeongyahak.domain.auth.enums.ProviderType;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.department.entity.Department;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
    @Setter
    @Column(nullable = false, length = 50)
    private String name;

    @Setter
    @Column(length = 20)
    private String phoneNumber;

    @Setter
    @Column(unique = true, length = 100, name = "primary_email")
    private String email;

    @Setter
    @Column(length = 255, name = "profile_image_url")
    private String profileImageUrl;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = true)
    private Department department;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = true)
    private Classroom classroom;

    @Setter
    @Column(length = 6, name = "resident_registration_number_prefix")
    private String residentRegistrationNumberPrefix;

    @Setter
    @Column(name = "teacher_start_at")
    private LocalDate teacherStartAt;

    @Setter
    @Column(name = "teacher_end_at")
    private LocalDate teacherEndAt;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleType role = RoleType.GUEST;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<UserCredential> credentials = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<UserPermission> permissions = new HashSet<>();

    @Builder
    public User(
        @NonNull String name,
        String phoneNumber,
        String email,
        String profileImageUrl,
        Department department,
        Classroom classroom,
        String residentRegistrationNumberPrefix,
        LocalDate teacherStartAt,
        LocalDate teacherEndAt,
        RoleType role
    ) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.department = department;
        this.classroom = classroom;
        this.residentRegistrationNumberPrefix = residentRegistrationNumberPrefix;
        this.teacherStartAt = teacherStartAt;
        this.teacherEndAt = teacherEndAt;
        this.role = (role != null) ? role : RoleType.GUEST;
    }

    public void approveTeacherProfile(LocalDate teacherStartAt, LocalDate teacherEndAt) {
        this.teacherStartAt = teacherStartAt;
        this.teacherEndAt = teacherEndAt;
        this.role = RoleType.VOLUNTEER;
    }

    public void releaseTeacherProfile(LocalDate teacherEndAt) {
        this.department = null;
        this.classroom = null;
        this.teacherEndAt = teacherEndAt;
        this.role = RoleType.GUEST;
    }

    public String getUsername() {
        return findLocalCredential()
            .map(UserCredential::getCredentialEmail)
            .orElse(null);
    }

    public String getPasswordHash() {
        return findLocalCredential()
            .map(UserCredential::getPasswordHash)
            .orElse(null);
    }

    public void addCredential(UserCredential credential) {
        this.credentials.add(credential);
    }

    public void addPermission(UserPermission permission) {
        this.permissions.add(permission);
    }

    public void clearPermissions() {
        this.permissions.clear();
    }

    private Optional<UserCredential> findLocalCredential() {
        return credentials.stream()
            .filter(credential -> credential.getProvider() == ProviderType.LOCAL)
            .findFirst();
    }
}
