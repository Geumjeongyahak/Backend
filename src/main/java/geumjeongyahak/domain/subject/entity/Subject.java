package geumjeongyahak.domain.subject.entity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.users.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "subjects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private LocalDate startAt;

    @Column(nullable = false)
    private LocalDate endAt;

    @Column(nullable = false)
    private Integer times;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Integer period;

    private LocalDate assignedFrom;

    private LocalDate assignedTo;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Subject(
            Classroom classroom,
            User teacher,
            String name,
            LocalDate startAt,
            LocalDate endAt,
            Integer times,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Integer period,
            LocalDate assignedFrom,
            LocalDate assignedTo,
            String description) {
        this.classroom = classroom;
        this.teacher = teacher;
        this.name = name;
        this.startAt = startAt;
        this.endAt = endAt;
        this.times = times;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.period = period;
        this.assignedFrom = assignedFrom;
        this.assignedTo = assignedTo;
        this.description = description;
    }

    public void update(
        Classroom classroom,
        User teacher,
        String name,
        LocalDate startAt,
        LocalDate endAt,
        Integer times,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer period,
        LocalDate assignedFrom,
        LocalDate assignedTo,
        String description
    ) {
        this.classroom = classroom;
        this.teacher = teacher;
        this.name = name;
        this.startAt = startAt;
        this.endAt = endAt;
        this.times = times;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.period = period;
        this.assignedFrom = assignedFrom;
        this.assignedTo = assignedTo;
        this.description = description;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void changeTeacher(User teacher) {
        this.teacher = teacher;
    }
}

