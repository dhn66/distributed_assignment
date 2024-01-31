import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.io.*;

public class NodeImpl implements Node, Serializable {

	protected ArrayList<BrokerImpl> brokers = new ArrayList<BrokerImpl>();
	Socket clientSocket;
	Socket updateNodesSocket;
	ObjectOutputStream out;
	ObjectInputStream in;

	private void writeObject(ObjectOutputStream stream) throws IOException{
		stream.writeObject(brokers);
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException{
		brokers=(ArrayList) stream.readObject();
	}

	public List<BrokerImpl> getBrokers() {
		return brokers;
	}

	public void init(){
		BrokerReader brokerReader = new BrokerReader();
		brokers = brokerReader.getBrokers();
	}

	public int brokerIndex(BrokerImpl respBroker){
		int i = 0;
		for (BrokerImpl broker: brokers){
			if(broker.id == respBroker.id){
				return i;
			}
			i++;
		}
		return -1;
	}

	public synchronized boolean connect(BrokerImpl broker) {
		try {
			clientSocket = new Socket(InetAddress.getLocalHost(), broker.port);
			out = new ObjectOutputStream(clientSocket.getOutputStream());
			in = new ObjectInputStream(clientSocket.getInputStream());
			return true;
		} catch (IOException e) {
			System.out.println("Failed to connect.");
			e.printStackTrace();
			return false;
		}	
	}

	public synchronized void disconnect() {
		try {
        	clientSocket.close();
		} catch (IOException e) {
			System.out.println("Failed to disconnect.");
			e.printStackTrace();
		}
		
	}

	public synchronized void updateNodes() {
		for (BrokerImpl broker: brokers){
			try {
				Socket updateNodesSocket = new Socket("127.0.0.1", broker.port);
				out = new ObjectOutputStream(updateNodesSocket.getOutputStream());

				out.writeObject("update");
				out.flush();

				out.writeObject(brokers);
				out.flush();

				updateNodesSocket.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			} 	
		}
	}
	
	public synchronized BigInteger hash(String hashtag) {
		BigInteger no=null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] messageDigest = md.digest(hashtag.getBytes());
	
			no = new BigInteger(1, messageDigest);
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return no;
	}
}