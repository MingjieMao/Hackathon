package moderation;

import dao.model.Message;
import dao.model.Report;

import java.util.*;

class MostStrategy implements ReportedMessageIteratorFactory {
    private static final class MessageReportCount {
        private final UUID messageId;
        private final int reportCount;

        private MessageReportCount(UUID messageId, int reportCount) {
            this.messageId = messageId;
            this.reportCount = reportCount;
        }
    }

    @Override
    public Iterator<Message> create(
            Map<UUID, Map<UUID, Report>> reports,
            Map<UUID, Message> messageMap,
            int amount) {

        List<MessageReportCount> entries = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, Report>> entry : reports.entrySet()) {
            int count = entry.getValue().size();
            if (count == 0) continue;
            entries.add(new MessageReportCount(entry.getKey(), count));
        }
        entries.sort((a, b) -> Integer.compare(b.reportCount, a.reportCount));

        List<Message> result = new ArrayList<>();
        for (MessageReportCount entry : entries) {
            Message m = messageMap.get(entry.messageId);
            if (m != null) result.add(m);
            if (result.size() == amount) break;
        }
        return new ReportedMessageIterator(result);
    }
}
