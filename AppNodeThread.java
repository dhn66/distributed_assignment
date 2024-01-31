import java.net.*;
import java.util.*;
import java.io.*;

public class AppNodeThread {

    public static void main(String[] args) {
        Socket requestSocket = null;

        try {
            BrokerReader brokerReader = new BrokerReader();
            Random rand = new Random();
            int br_id = rand.nextInt(3);
            requestSocket = new Socket("127.0.0.1", brokerReader.getBrokers().get(br_id).port);
            AppNode new_user = new AppNode(requestSocket, new Channel(args[0]));
            new_user.init();
            Thread t = new Thread(new_user);
            t.start();
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }         
    }
    
}
