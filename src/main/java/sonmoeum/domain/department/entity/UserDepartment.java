package sonmoeum.domain.department.entity;

import jakarta.persistence.*;
import sonmoeum.domain.users.entity.User;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "user_departments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    public UserDepartment(User user, Department department) {
        this.user = user;
        this.department = department;
    }
}
