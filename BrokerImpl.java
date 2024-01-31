import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeoutException;


public class BrokerImpl extends NodeImpl implements Runnable, Serializable {
	ObjectOutputStream out;
	ObjectInputStream in;
	protected  HashMap<String, AppNode> registeredUsers = new HashMap<String, AppNode>();
	protected  List<AppNode> registeredPublishers = new ArrayList<AppNode>();
	protected  ArrayList<String> channels = new ArrayList<String>();
	protected  ArrayList<String> hashtags = new ArrayList<String>();
	int port;
	int id;




	private void writeObject(ObjectOutputStream stream) throws IOException{
		stream.writeObject(id);
		stream.writeObject(port);
		stream.writeObject(registeredUsers);
		stream.writeObject(registeredPublishers);
		stream.writeObject(channels);
		stream.writeObject(hashtags);
		stream.writeObject(brokers);
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException{
		id = (int) stream.readObject();
		port = (int) stream.readObject();
		registeredUsers = (HashMap) stream.readObject();
		registeredPublishers = (ArrayList) stream.readObject();
		channels = (ArrayList) stream.readObject();
		hashtags = (ArrayList) stream.readObject();
		brokers=(ArrayList) stream.readObject();
	}


	BrokerImpl(){}
	BrokerImpl(int id) {
    	this.id = id;
    }
	
	BrokerImpl(Socket socket, int id,int port){
    	this.port = port;
    	this.id = id;
		try{
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			
		} catch (IOException ioException) {
            ioException.printStackTrace();
        } 
	}

	
	public synchronized String pull(String name){

		for (AppNode publisher:registeredPublishers) {
			File file = new File("videos");
			boolean made = file.mkdir();
			try(OutputStream writer = new FileOutputStream("videos/complete_video");
			ObjectInputStream temp_in = new ObjectInputStream(publisher.appNodeSocket.getInputStream())) {
				publisher.push(name);
				if (made) {
					boolean done = true;
					while(done) {
						File video_chunk = (File) temp_in.readObject();
						if (video_chunk==null)
							done=false;
						InputStream reader = new FileInputStream(video_chunk);
						int readByte=0;
						while((readByte = reader.read()) != -1) {
							writer.write(readByte);
						}
						reader.close();
					}
				}
				else
					System.out.println("Failed to create directory for incoming video file - try again.");
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		String path = "C:\\Users\\A\\Desktop\\AUEB\\Distributed_Systems\\ErgasiaA\\videos\\complete_video";
		return path;	
	}

	public synchronized BrokerImpl findBroker(String topic, String filter) {
		for (BrokerImpl broker : brokers) {
			if(filter.equals("channel")){
				for (AppNode publisher : registeredPublishers) {
					if (topic.equals(publisher.channel.channelName))
						return broker;
				}	
			}
			else{
				for (String hashtag : hashtags) {
					if (topic.equals(hashtag))
						return broker;	
				}
			}
		}
		return null;
	}
	

	public void run() {

		try {
			while (true) {
				String action = (String) in.readObject();

				if (action.equals("1") || action.equals("2")) {
					String answer = (String) in.readObject();
					if (answer.equals("abort")) {
						continue;
					}
					BrokerImpl respBroker = null;
					if (action.equals("1")) {
						if (channels.contains(answer)) {
							respBroker = this;
						} else {
							respBroker = findBroker(answer, "channel");
						}
					} else {
						if (hashtags.contains(answer)) {
							respBroker = this;
						} else {
							respBroker = findBroker(answer, "hashtag");
						}
					}

					out.writeObject(respBroker);
					out.flush();

					if (respBroker != null) {
						AppNode subscriber = (AppNode) in.readObject();
						int brokerIndex = brokerIndex(respBroker);
						if (!respBroker.registeredUsers.keySet().contains(subscriber)) {
							brokers.get(brokerIndex).registeredUsers.put(answer, subscriber);
							if (respBroker.equals(this))
								registeredUsers.put(answer, subscriber);
						}
						System.out.println("User registered successfully to according broker.");
						updateNodes();
					} else {
						System.out.println("Couldn't find specified topic in any broker!");
					}
				}
				else if (action.equals("3") || action.equals("4") || action.equals("5")) {
					out.writeObject(this.brokers);
					out.flush();
				}
				else if (action.equals("update")) {
					this.brokers = (ArrayList) in.readObject();
					for (String hashtag: hashtags) {
						if(!brokers.get(id-1).hashtags.contains(hashtag))
							this.hashtags.remove(hashtag);
					}
					for(String hashtag:brokers.get(id-1).hashtags) {
						if(!this.hashtags.contains(hashtag)) {
							this.hashtags.add(hashtag);
						}
					}
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}