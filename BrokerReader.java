import java.util.*;
import java.io.*;

public class BrokerReader {

    public int thisBroker(String id) {
        try {
            File myObj = new File("broker_info.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                if(data.substring(4).equals(id)){
                    data = myReader.nextLine().substring(6,10);
                    return Integer.parseInt(data);
                }
            }
            myReader.close();

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return -1;
    }

    public ArrayList<BrokerImpl> getBrokers(){
        ArrayList<BrokerImpl> brokers = new ArrayList<BrokerImpl>();
        try {

            File myObj = new File("broker_info.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                if(data.substring(0,2).equals("id")){
                    BrokerImpl broker = new BrokerImpl();
                    broker.id = Integer.parseInt(data.substring(4));
                    data = myReader.nextLine();
                    broker.port =  Integer.parseInt(data.substring(6,10));
                    brokers.add(broker);
                }
            }
            myReader.close();

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return brokers;
    }
}
