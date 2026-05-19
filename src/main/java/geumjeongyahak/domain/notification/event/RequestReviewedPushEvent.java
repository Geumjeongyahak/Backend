package geumjeongyahak.domain.notification.event;

import geumjeongyahak.common.event.dto.BaseEventDto;
import geumjeongyahak.domain.notification.enums.PushRequestType;
import geumjeongyahak.domain.notification.enums.PushReviewResult;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class RequestReviewedPushEvent extends BaseEventDto {

    private final Long userId;
    private final Long requestId;
    private final PushRequestType requestType;
    private final PushReviewResult reviewResult;
    private final Long reviewerId;
    private final String title;
    private final String body;
    private final String note;

    public RequestReviewedPushEvent(
        Long userId,
        Long requestId,
        PushRequestType requestType,
        PushReviewResult reviewResult,
        Long reviewerId,
        String title,
        String body,
        String note
    ) {
        this.userId = userId;
        this.requestId = requestId;
        this.requestType = requestType;
        this.reviewResult = reviewResult;
        this.reviewerId = reviewerId;
        this.title = title;
        this.body = body;
        this.note = note;
    }

    public static RequestReviewedPushEvent approved(
        Long userId,
        Long requestId,
        PushRequestType requestType,
        Long reviewerId,
        String title,
        String body,
        String note
    ) {
        return new RequestReviewedPushEvent(
            userId,
            requestId,
            requestType,
            PushReviewResult.APPROVED,
            reviewerId,
            title,
            body,
            note
        );
    }

    public static RequestReviewedPushEvent rejected(
        Long userId,
        Long requestId,
        PushRequestType requestType,
        Long reviewerId,
        String title,
        String body,
        String note
    ) {
        return new RequestReviewedPushEvent(
            userId,
            requestId,
            requestType,
            PushReviewResult.REJECTED,
            reviewerId,
            title,
            body,
            note
        );
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("requestId", requestId);
        data.put("requestType", requestType);
        data.put("reviewResult", reviewResult);
        data.put("reviewerId", reviewerId);
        data.put("title", title);
        data.put("body", body);
        data.put("note", note);
        return data;
    }
}
