import java.net.*;
import java.util.*;
import java.io.*;

public class BrokerThread {


    public static void main(String[] args) {
        ServerSocket providerSocket;
        Socket connection = null;

        try {
			BrokerReader brokerReader = new BrokerReader();
			int port = brokerReader.thisBroker(args[0]);
            providerSocket = new ServerSocket(port , 10);
            System.out.println("Server is on");

			while (true) {
				connection = providerSocket.accept();
                BrokerImpl new_broker = new BrokerImpl(connection, Integer.parseInt(args[0]), providerSocket.getLocalPort());
                new_broker.init();
				Thread h = new Thread(new_broker);
				h.start();
                System.out.println("Thread "  + h.getId() + " Created successfully");
			}
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } 
    }
}


