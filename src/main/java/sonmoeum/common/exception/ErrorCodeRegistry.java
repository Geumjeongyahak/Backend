package sonmoeum.common.exception;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import sonmoeum.domain.auth.exception.AuthErrorCode;
import sonmoeum.domain.channel.exception.ChannelErrorCode;
import sonmoeum.domain.classroom.exception.ClassroomErrorCode;
import sonmoeum.domain.comment.exception.CommentErrorCode;
import sonmoeum.domain.department.exception.DepartmentErrorCode;
import sonmoeum.domain.lesson.exception.LessonErrorCode;
import sonmoeum.domain.post.exception.PostErrorCode;
import sonmoeum.domain.request.exception.RequestErrorCode;
import sonmoeum.domain.student.exception.StudentErrorCode;
import sonmoeum.domain.subject.exception.SubjectErrorCode;
import sonmoeum.domain.users.exception.UserErrorCode;

public final class ErrorCodeRegistry {

    private ErrorCodeRegistry() {
    }

    public static List<ErrorCode> getAll() {
        return Stream.of(
                CommonErrorCode.values(),
                AuthErrorCode.values(),
                UserErrorCode.values(),
                StudentErrorCode.values(),
                DepartmentErrorCode.values(),
                ClassroomErrorCode.values(),
                LessonErrorCode.values(),
                SubjectErrorCode.values(),
                ChannelErrorCode.values(),
                PostErrorCode.values(),
                CommentErrorCode.values(),
                RequestErrorCode.values()
            )
            .flatMap(Arrays::stream)
            .map(ErrorCode.class::cast)
            .toList();
    }
}
