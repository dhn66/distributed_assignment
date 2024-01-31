public interface Broker extends Node{	
	
	public AppNode acceptConnection(AppNode appNode);
	
	public void notifyPublisher(String publisher);
	
	public String pull(String name);
	
	public Broker filterConsumers(String filter);
	
	public Broker findBroker(String topic, String filters);
}