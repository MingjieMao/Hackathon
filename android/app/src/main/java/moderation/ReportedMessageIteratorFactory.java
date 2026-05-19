package moderation;

import dao.model.Message;
import dao.model.Report;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

interface ReportedMessageIteratorFactory {
    Iterator<Message> create(Map<UUID, Map<UUID, Report>> reports, Map<UUID, Message> messageMap, int amount);
}
