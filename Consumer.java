public interface Consumer extends Node{
	
	void register(Broker broker, String topic);
	
	void disconnect(Broker broker, String topic);
	
	void playData(String topic, Value video);
}