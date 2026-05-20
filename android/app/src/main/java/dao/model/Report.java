package dao.model;

import java.util.Objects;
import java.util.UUID;

public final class Report implements HasUUID{
    public final UUID message;
    public final UUID user;
    public final long timestamp;

    public Report(UUID messageId, UUID userId, long timestamp) {
        this.message = messageId;
        this.user = userId;
        this.timestamp = timestamp;
    }

    @Override
    public UUID getUUID(){
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Report other)) return false;
        return message.equals(other.message) && user.equals(other.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, user);
    }
}