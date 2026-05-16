package geumjeongyahak.domain.notification.dto;

public record PushSendResult(
    boolean success,
    boolean invalidToken,
    String errorMessage
) {
    public static PushSendResult succeeded() {
        return new PushSendResult(true, false, null);
    }

    public static PushSendResult failed(boolean invalidToken, String errorMessage) {
        return new PushSendResult(false, invalidToken, errorMessage);
    }
}
