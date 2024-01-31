import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.io.*;

public class Channel implements Serializable {
	public String channelName;
	public HashMap<String, Integer> hashtagsPublished;
	public ArrayList<VideoFile> videosPublished;
	public HashMap<String, VideoFile> userVideoFilesMap;
	public VideoFile videoFile;

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.writeObject(channelName);
		stream.writeObject(videoFile);
		stream.writeObject(hashtagsPublished);
		stream.writeObject(videosPublished);
		stream.writeObject(userVideoFilesMap);
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		channelName = (String) stream.readObject();
		videoFile = (VideoFile)  stream.readObject();
		hashtagsPublished = (HashMap) stream.readObject();
		videosPublished = (ArrayList) stream.readObject();
		userVideoFilesMap = (HashMap) stream.readObject();
	}
	
	Channel(String channelName) {
		this.channelName=channelName;
		this.hashtagsPublished= new HashMap<String, Integer>();
		this.userVideoFilesMap= new HashMap<String, VideoFile>();
		this.videosPublished = new ArrayList<VideoFile>();
	}
}


