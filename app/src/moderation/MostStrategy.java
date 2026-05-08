package moderation;

import dao.model.Message;
import dao.model.Report;

import java.util.*;

class MostStrategy implements ReportedMessageIteratorFactory {
    @Override
    public Iterator<Message> create(
            Map<UUID, Map<UUID, Report>> reports,
            Map<UUID, Message> messageMap,
            int amount) {

        List<Map.Entry<UUID, Integer>> entries = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, Report>> entry : reports.entrySet()) {
            int count = entry.getValue().size();
            if (count == 0) continue;
            entries.add(Map.entry(entry.getKey(), count));
        }
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        List<Message> result = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : entries) {
            Message m = messageMap.get(e.getKey());
            if (m != null) result.add(m);
            if (result.size() == amount) break;
        }
        return new ReportedMessageIterator(result);
    }
}
