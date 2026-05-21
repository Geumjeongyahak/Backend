package geumjeongyahak.common.exception;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import geumjeongyahak.domain.auth.exception.AuthErrorCode;
import geumjeongyahak.domain.channel.exception.ChannelErrorCode;
import geumjeongyahak.domain.classroom.exception.ClassroomErrorCode;
import geumjeongyahak.domain.comment.exception.CommentErrorCode;
import geumjeongyahak.domain.department.exception.DepartmentErrorCode;
import geumjeongyahak.domain.lesson.exception.LessonErrorCode;
import geumjeongyahak.domain.post.exception.PostErrorCode;
import geumjeongyahak.domain.request.exception.RequestErrorCode;
import geumjeongyahak.domain.student.exception.StudentErrorCode;
import geumjeongyahak.domain.subject.exception.SubjectErrorCode;
import geumjeongyahak.domain.users.exception.UserErrorCode;

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
