package moderation;

import dao.model.Message;
import dao.model.Report;

import java.util.*;

class OldestStrategy implements ReportedMessageIteratorFactory {
    private static final class MessageOldestReport {
        private final UUID messageId;
        private final long oldestTimestamp;

        private MessageOldestReport(UUID messageId, long oldestTimestamp) {
            this.messageId = messageId;
            this.oldestTimestamp = oldestTimestamp;
        }
    }

    @Override
    public Iterator<Message> create(
            Map<UUID, Map<UUID, Report>> reports,
            Map<UUID, Message> messageMap,
            int amount) {

        List<MessageOldestReport> entries = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, Report>> entry : reports.entrySet()) {
            Map<UUID, Report> byUser = entry.getValue();
            if (byUser.isEmpty()) continue;
            long oldest = Long.MAX_VALUE;
            for (Report report : byUser.values()) {
                if (report.timestamp < oldest) {
                    oldest = report.timestamp;
                }
            }
            entries.add(new MessageOldestReport(entry.getKey(), oldest));
        }
        entries.sort(Comparator.comparingLong(item -> item.oldestTimestamp));

        List<Message> result = new ArrayList<>();
        for (MessageOldestReport entry : entries) {
            Message m = messageMap.get(entry.messageId);
            if (m != null) result.add(m);
            if (result.size() == amount) break;
        }
        return new ReportedMessageIterator(result);
    }
}
