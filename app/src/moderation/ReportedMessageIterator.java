package moderation;

import dao.model.Message;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

class ReportedMessageIterator implements Iterator<Message> {
    private final List<Message> messages;
    private int index = 0;

    ReportedMessageIterator(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public boolean hasNext() {
        return index < messages.size();
    }

    @Override
    public Message next() {
        if (!hasNext()) throw new NoSuchElementException();
        return messages.get(index++);
    }
}
