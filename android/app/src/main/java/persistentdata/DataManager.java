package persistentdata;


import dao.PostDAO;
import dao.UserDAO;
import dao.model.Message;
import dao.model.Post;
import dao.model.Report;
import dao.model.User;
import moderation.ModerationTools;
import persistentdata.formatted.CSVFormat;
import persistentdata.formatted.CSVFormattedFactory;
import persistentdata.io.ComputerIOFactory;
import persistentdata.io.IOFactory;
import persistentdata.serialization.HiddenMessageSerializer;
import persistentdata.serialization.MessageSerializer;
import persistentdata.serialization.PostSerializer;
import persistentdata.serialization.ReportSerializer;
import persistentdata.serialization.UserSerializer;


import java.util.UUID;


public class DataManager {
	private static DataManager instance;
	public static DataManager getInstance() {
		if (instance == null)
			instance = new DataManager();
		return instance;
	}


	private final IOFactory IO = new ComputerIOFactory();


	private final DataPipeline<User, String[]> userPipeline = new DataPipeline<>(
			IO, new CSVFormattedFactory(new CSVFormat(4)), new UserSerializer(), "users");


	private final DataPipeline<Post, String[]> postPipeline = new DataPipeline<>(
			IO, new CSVFormattedFactory(new CSVFormat(3)), new PostSerializer(), "posts");


	private final DataPipeline<Message, String[]> messagePipeline = new DataPipeline<>(
			IO, new CSVFormattedFactory(new CSVFormat(5)), new MessageSerializer(), "messages");


	private final DataPipeline<Report, String[]> reportPipeline = new DataPipeline<>(
			IO, new CSVFormattedFactory(new CSVFormat(3)), new ReportSerializer(), "reports");


	private final DataPipeline<UUID, String[]> hiddenPipeline = new DataPipeline<>(
			IO, new CSVFormattedFactory(new CSVFormat(1)), new HiddenMessageSerializer(), "hidden");

	private final UserDAO users = UserDAO.getInstance();
	private final PostDAO posts = PostDAO.getInstance();


	public void readAll() {
		users.clear();
		posts.clear();
		ModerationTools.clearAll();
		userPipeline.readTo(users::add);
		postPipeline.readTo(posts::add);
		messagePipeline.readTo((message) -> posts.get(new Post(message.thread())).messages.insert(message));
		reportPipeline.readTo(ModerationTools::loadReport);
		hiddenPipeline.readTo(ModerationTools::loadHidden);
	}


	public void writeAll() {
		userPipeline.writeFrom(users.getAll());
		postPipeline.writeFrom(posts.getAll());
		messagePipeline.writeFrom(posts.getAllMessages());
		reportPipeline.writeFrom(ModerationTools.getAllReports());
		hiddenPipeline.writeFrom(ModerationTools.getAllHiddenIds());
	}
}


