package moderation;

import dao.model.Message;
import dao.model.Report;

import java.util.*;

class OldestStrategy implements ReportedMessageIteratorFactory {
    @Override
    public Iterator<Message> create(
            Map<UUID, Map<UUID, Report>> reports,
            Map<UUID, Message> messageMap,
            int amount) {

        List<Map.Entry<UUID, Long>> entries = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, Report>> entry : reports.entrySet()) {
            Map<UUID, Report> byUser = entry.getValue();
            if (byUser.isEmpty()) continue;
            long oldest = byUser.values().stream()
                    .mapToLong(r -> r.timestamp)
                    .min().getAsLong();
            entries.add(Map.entry(entry.getKey(), oldest));
        }
        entries.sort(Comparator.comparingLong(Map.Entry::getValue));

        List<Message> result = new ArrayList<>();
        for (Map.Entry<UUID, Long> e : entries) {
            Message m = messageMap.get(e.getKey());
            if (m != null) result.add(m);
            if (result.size() == amount) break;
        }
        return new ReportedMessageIterator(result);
    }
}
