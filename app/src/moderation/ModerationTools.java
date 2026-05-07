package moderation;

import dao.model.Message;
import java.util.Iterator;
import java.util.UUID;

public class ModerationTools {
	public static boolean addReport(UUID message, UUID user, long timestamp) {
		// TODO: task 1
		return false;
	}
	
	public static boolean removeReport(UUID message, UUID user, long timestamp) {
		// TODO: task 1
		return false;
	}
	
	public static boolean hasReported(UUID message, UUID user) {
		// TODO: task 1
		return false;
	}
	
	public static boolean setHidden(UUID message, UUID user, boolean hidden) {
		// TODO: task 2
		return false;
	}
	
	public static Iterator<Message> getReportedMessages(String strategy, int amount) {
		// TODO: task 4
		return null;
	}
}
