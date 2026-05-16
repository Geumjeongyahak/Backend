package geumjeongyahak.domain.notification.dto;

import geumjeongyahak.domain.notification.event.PurchaseStatusChangedPushEvent;
import geumjeongyahak.domain.notification.event.RequestReviewedPushEvent;

import java.util.HashMap;
import java.util.Map;

public record PushNotificationMessage(
    String title,
    String body,
    Map<String, String> data
) {
    public static PushNotificationMessage from(RequestReviewedPushEvent event) {
        Map<String, String> data = new HashMap<>();
        data.put("eventType", "REQUEST_REVIEWED");
        data.put("requestId", String.valueOf(event.getRequestId()));
        data.put("requestType", event.getRequestType().name());
        data.put("reviewResult", event.getReviewResult().name());
        data.put("reviewerId", String.valueOf(event.getReviewerId()));
        if (event.getNote() != null) {
            data.put("note", event.getNote());
        }

        return new PushNotificationMessage(event.getTitle(), event.getBody(), data);
    }

    public static PushNotificationMessage from(PurchaseStatusChangedPushEvent event) {
        Map<String, String> data = new HashMap<>();
        data.put("eventType", "PURCHASE_STATUS_CHANGED");
        data.put("requestId", String.valueOf(event.getRequestId()));
        data.put("newStatus", event.getNewStatus());
        return new PushNotificationMessage(event.getTitle(), event.getBody(), data);
    }
}
