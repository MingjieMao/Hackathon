package moderation;


import dao.PostDAO;
import dao.UserDAO;
import dao.model.Message;
import dao.model.Report;
import dao.model.User;


import java.util.*;


public class ModerationTools {
	private static final Map<UUID, Map<UUID, Report>> reports = new HashMap<>();

    // Task 2: hiddenMessages UUID。
    private static final Set<UUID> hiddenMessages = new HashSet<>();

	public static boolean addReport(UUID message, UUID user, long timestamp) {
		if (findMessage(message) == null) return false;
		if (UserDAO.getInstance().getByUUID(user) == null) return false;


		reports.putIfAbsent(message, new HashMap<>());
		Map<UUID, Report> byUser = reports.get(message);
		if (byUser.containsKey(user)) return false;


		byUser.put(user, new Report(message, user, timestamp));
		return true;
	}


	public static boolean removeReport(UUID message, UUID user, long timestamp) {
		if (findMessage(message) == null) return false;
		if (UserDAO.getInstance().getByUUID(user) == null) return false;


		Map<UUID, Report> byUser = reports.get(message);
		if (byUser == null || !byUser.containsKey(user)) return false;


		byUser.remove(user);
		return true;
	}


	public static boolean hasReported(UUID message, UUID user) {
		Map<UUID, Report> byUser = reports.get(message);
		return byUser != null && byUser.containsKey(user);
	}

    // task 2
    public static boolean setHidden(UUID message, UUID user, boolean hidden) {
        User foundUser = UserDAO.getInstance().getByUUID(user);
        if (foundUser == null) return false;
        if (foundUser.role() != User.Role.Admin) return false;

        if (!messageExists(message)) return false;

        // Update message state
        if (hidden) {
            hiddenMessages.add(message);
        } else {
            hiddenMessages.remove(message);
        }
        return true;
    }

    // To Post.getVisibleMessages
    public static boolean isHidden(UUID message) {
        return hiddenMessages.contains(message);
    }

    private static boolean messageExists(UUID message) {
        if (message == null) return false;

        Iterator<Message> allMessages = PostDAO.getInstance().getAllMessages();
        while (allMessages.hasNext()) {
            if (allMessages.next().id().equals(message)) return true;
        }
        return false;
    }

    private static boolean userExists(UUID user) {
        if (user == null) return false;
        return UserDAO.getInstance().getByUUID(user) != null;
    }


	public static Iterator<Message> getReportedMessages(String strategy, int amount) {
		// TODO: task 4
		return null;
	}


	private static Message findMessage(UUID messageId) {
		Iterator<Message> it = PostDAO.getInstance().getAllMessages();
		while (it.hasNext()) {
			Message m = it.next();
			if (m.id().equals(messageId)) return m;
		}
		return null;
	}
}
