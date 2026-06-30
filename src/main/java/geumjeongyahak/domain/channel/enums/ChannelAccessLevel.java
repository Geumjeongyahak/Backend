package geumjeongyahak.domain.channel.enums;

public enum ChannelAccessLevel {
    CLOSED(1),
    READ_ONLY(2),
    READ_COMMENT(3),
    READ_WRITE(4);

    private int priority;

    ChannelAccessLevel(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
