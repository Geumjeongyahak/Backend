package sonmoeum.domain.student.entity;

import sonmoeum.domain.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "students")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    public Student(String name, String phoneNumber, String description) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.description = description;
    }

    public void update(String name, String phoneNumber, String description) {
        if (name != null) {
            this.name = name;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
        if (description != null) {
            this.description = description;
        }
    }
}
