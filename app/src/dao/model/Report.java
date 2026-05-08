package dao.model;

import java.util.Objects;
import java.util.UUID;

public final class Report {
    public final UUID messageId;
    public final UUID userId;
    public final long timestamp;

    public Report(UUID messageId, UUID userId, long timestamp) {
        this.messageId = messageId;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Report other)) return false;
        return messageId.equals(other.messageId) && userId.equals(other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, userId);
    }
}