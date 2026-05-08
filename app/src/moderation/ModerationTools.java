package moderation;


import dao.PostDAO;
import dao.UserDAO;
import dao.model.Message;
import dao.model.Report;
import dao.model.User;


import java.util.*;


public class ModerationTools {
	private static final Map<UUID, Map<UUID, Report>> reports = new HashMap<>();
	private static final Set<UUID> hiddenMessages = new HashSet<>();


	// ── task 1 ──────────────────────────────────────────────────────────────


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


	// ── task 2 ──────────────────────────────────────────────────────────────


	public static boolean setHidden(UUID message, UUID user, boolean hidden) {
		if (findMessage(message) == null) return false;
		User u = UserDAO.getInstance().getByUUID(user);
		if (u == null || u.role() != User.Role.Admin) return false;


		if (hidden) hiddenMessages.add(message);
		else hiddenMessages.remove(message);
		return true;
	}


	public static boolean isHidden(UUID message) {
		return hiddenMessages.contains(message);
	}


	// ── task 4 (stub) ────────────────────────────────────────────────────────


	public static Iterator<Message> getReportedMessages(String strategy, int amount) {
		// TODO: task 4
		return null;
	}


	// ── persistence helpers (task 3) ─────────────────────────────────────────


	public static Iterator<Report> getAllReports() {
		List<Report> all = new ArrayList<>();
		for (Map<UUID, Report> byUser : reports.values()) {
			all.addAll(byUser.values());
		}
		return all.iterator();
	}


	public static Iterator<UUID> getAllHiddenIds() {
		return new ArrayList<>(hiddenMessages).iterator();
	}


	public static void loadReport(Report r) {
		reports.putIfAbsent(r.message, new HashMap<>());
		reports.get(r.message).put(r.user, r);
	}


	public static void loadHidden(UUID messageId) {
		hiddenMessages.add(messageId);
	}


	public static void clearAll() {
		reports.clear();
		hiddenMessages.clear();
	}


	// ── private helpers ──────────────────────────────────────────────────────


	private static Message findMessage(UUID messageId) {
		Iterator<Message> it = PostDAO.getInstance().getAllMessages();
		while (it.hasNext()) {
			Message m = it.next();
			if (m.id().equals(messageId)) return m;
		}
		return null;
	}
}
