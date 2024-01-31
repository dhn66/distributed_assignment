import java.math.BigInteger;
import java.net.Socket;
import java.util.List;

public interface Node{
	List<BrokerImpl> getBrokers();
	boolean connect(BrokerImpl broker);
	void disconnect();
	void updateNodes();
	BigInteger hash(String hashtag);
}