import java.util.ArrayList;

public interface Publisher extends Node {
	
		Channel channel=null;

		void addHashTag(String hashtag, VideoFile video);

		void removeHashTag(String hashtag, VideoFile video);

		BrokerImpl hashTopic(String topic);

		void notifyBrokersForAddedHashTags(String hashtag);

		void notifyBrokersForRemovedHashtags(String hashtag);

		void push(String name);

		VideoFile upload (String videoName);

		void generateChunks(String videoName);
}