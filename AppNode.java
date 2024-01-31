/*import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp4.MP4Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;*/

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppNode extends NodeImpl implements Runnable, Serializable {
    int id;
    ObjectOutputStream out;
	ObjectInputStream in;
    Socket appNodeSocket;

    public Channel channel;
    protected  ArrayList<BrokerImpl> associatedBrokers = new ArrayList<BrokerImpl>();
	protected  ArrayList<BrokerImpl> registeredBrokers = new ArrayList<BrokerImpl>();



    private void writeObject(ObjectOutputStream stream) throws IOException{
        stream.writeObject(id);
        stream.writeObject(channel);
		stream.writeObject(associatedBrokers);
		stream.writeObject(registeredBrokers);
		stream.writeObject(brokers);


	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException{
        id = (int) stream.readObject();
        channel = (Channel) stream.readObject();
		associatedBrokers = (ArrayList) stream.readObject();
		registeredBrokers = (ArrayList) stream.readObject();
		brokers=(ArrayList) stream.readObject();
	}
    
    AppNode(Socket socket, Channel channel){
        this.channel = channel;
        id = 0;
		try{
            appNodeSocket=socket;
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
		} catch (IOException ioException) {
            ioException.printStackTrace();
        } 
	}
    //Publisher Methods
	public synchronized void addHashTag(String hashtag, VideoFile video) {
        if(!channel.hashtagsPublished.keySet().contains(hashtag)){
            channel.hashtagsPublished.put(hashtag, 1);
        }
		channel.hashtagsPublished.put(hashtag, channel.hashtagsPublished.get(hashtag) + 1);
        channel.videosPublished.get(channel.videosPublished.indexOf(video)).getAssociatedHashtags().add(hashtag);
    
		notifyBrokersForAddedHashTags(hashtag);
	}
		
	public synchronized void removeHashTag(String hashtag, VideoFile video) {
		channel.hashtagsPublished.put(hashtag, channel.hashtagsPublished.get(hashtag) - 1);
        if (channel.hashtagsPublished.get(hashtag) == 0){
            channel.hashtagsPublished.remove(hashtag);
        }
        channel.videosPublished.get(channel.videosPublished.indexOf(video)).getAssociatedHashtags().remove(hashtag);
        notifyBrokersForRemovedHashtags(hashtag);
	}

	public synchronized BrokerImpl hashTopic(String topic){
		BigInteger hash = hash(topic);
		BrokerImpl respBroker = new BrokerImpl();
        BigInteger diff = new BigInteger(String.valueOf(0));
		for (BrokerImpl broker:brokers) {
            BigInteger brokerHash = hash(String.valueOf(broker.port));
            BigInteger x = hash.subtract(brokerHash).abs();
            if (x.mod(brokerHash).compareTo(hash) <= 0) {
                respBroker = broker;
            }
		}
		return respBroker;
	}
		
	public synchronized void notifyBrokersForAddedHashTags(String hashtag){
        BrokerImpl respBroker = hashTopic(hashtag);
        int brokerIndex = brokerIndex(respBroker);
        if(!brokers.get(brokerIndex).hashtags.contains(hashtag))
            brokers.get(brokerIndex).hashtags.add(hashtag);
        if(!brokers.get(brokerIndex).registeredPublishers.contains(this))
            brokers.get(brokerIndex).registeredPublishers.add(this);
	}

    public synchronized void notifyBrokersForRemovedHashtags(String hashtag){
        BrokerImpl respBroker = hashTopic(hashtag);
        for(String brokerHashtag: respBroker.hashtags){
            for(String publisherHashtag: channel.hashtagsPublished.keySet()){
                if(publisherHashtag.equals(brokerHashtag)){
                    return;
                }
            }
            brokers.get(brokers.indexOf(respBroker)).registeredPublishers.remove(this);
        }
    }
    
    public synchronized void push(String name) {
        List<File> all_videos = new ArrayList<File>();
        try (Stream<Path> paths = Files.walk(Paths.get("/videos"))) {
            all_videos = paths.filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        } 
        //pushes a channel's content
        if (name.equals(this.channel.channelName)) {
            for(File file:all_videos) {
                generateChunks(file.getName());
                try (Stream<Path> vid_path= Files.walk(Paths.get("/videos/"+ file.getName()))) {
                    all_videos = vid_path.filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toList());
                    Stream<File> chunks = all_videos.stream();
                    Iterator<File> it = chunks.iterator();
                    while (it.hasNext()) {
                        out.writeObject(it.next());
                        out.flush();
                    }
                    out.writeObject(null);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //pushes a hashtag's content
        else {
            Iterator mp = this.channel.userVideoFilesMap.entrySet().iterator();
            while(mp.hasNext()) {
                Map.Entry<String, VideoFile> pair = (Map.Entry<String, VideoFile>)mp.next();
                VideoFile this_video = (VideoFile) pair.getValue();
                for (String hashtag: this_video.getAssociatedHashtags()) {
                    if (hashtag.equals(name)) {
                        for (File file:all_videos) {
                            if (file.getName().equals(this_video.getVideoName()))
                                generateChunks(file.getName());
                            try (Stream<Path> vid_path= Files.walk(Paths.get("/videos/"+ file.getName()))) {
                                all_videos = vid_path.filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toList());
                                Stream<File> chunks = all_videos.stream();
                                Iterator<File> it = chunks.iterator();
                                while(it.hasNext()) {
                                    out.writeObject(it.next());
                                    out.flush();
                                }
            
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } 
                    }
                }
                mp.remove();
            }
        }
    }

    public synchronized VideoFile upload (String videoName) throws IOException {
        //detecting the file type
        /*BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        FileInputStream inputstream = new FileInputStream(new File("User" +id + "Videos\\" + videoName +".mp4"));
        ParseContext pcontext = new ParseContext();
        
        //Html parser
        MP4Parser mp4Parser = new MP4Parser();
        mp4Parser.parse(inputstream, handler, metadata, pcontext);
        String[] keys = metadata.names();

        String duration = "";
        String dateCreated = "";
        String length = "";
        String framerate = "";
        String frameWidth = "";
        String frameHeight = "";

        for (String name: keys) {
            if (name.contains("duration")) {
                length = metadata.get(name);
                break;
            }
        }

        for (String name: keys) {
            if (name.contains("creation-date")) {
                dateCreated = metadata.get(name);
                break;
            }
        }

        for (String name: keys) {
            if (name.contains("ImageLength")) {
                frameHeight = metadata.get(name);
                break;
            }
        }

        for (String name: keys) {
            if (name.contains("ImageWidth")) {
                frameWidth = metadata.get(name);
                break;
            }
        }

        for (String name: keys) {
            if (name.contains("duration")) {
                duration = metadata.get(name);
                break;
            }
        }*/

        //VideoFile new_video = new VideoFile(videoName, channel.channelName, dateCreated, length, "", frameWidth, frameHeight);
        File file = new File("User" +id + "Videos\\" + videoName +".mp4");
        VideoFile new_video=new VideoFile(file.getName());
        channel.userVideoFilesMap.put(new_video.getVideoName(), new_video);
        channel.videosPublished.add(new_video);
        System.out.println("Video successfully uploaded");
        return new_video;

    }

	public synchronized void generateChunks(String videoName) {
		try {
            File file = new File("videos\\" + videoName);
            if (file.exists()) {
				String videoFileName = file.getName().substring(0, file.getName().lastIndexOf(".")); // Name of the videoFile without extension
				File splitFile = new File("User" + id + "Videos_Split\\"+ videoFileName);//Destination folder to save.
            if (!splitFile.exists()) {
                splitFile.mkdirs();
                System.out.println("Directory Created -> "+ splitFile.getAbsolutePath());
            }

            int i = 01;// Files count starts from 1
            InputStream inputStream = new FileInputStream(file);
            String videoFile = splitFile.getAbsolutePath() +"/"+ String.format("%02d", i) +"_"+ file.getName();// Location to save the files which are Split from the original file.
            OutputStream outputStream = new FileOutputStream(videoFile);
            System.out.println("File Created Location: "+ videoFile);
            int totalPartsToSplit = 20;// Total files to split.
            int splitSize = inputStream.available() / totalPartsToSplit;
            int streamSize = 0;
            int read = 0;
            while ((read = inputStream.read()) != -1) {

                if (splitSize == streamSize) {
                    if (i != totalPartsToSplit) {
                        i++;
                        String fileCount = String.format("%02d", i); // output will be 1 is 01, 2 is 02
                        videoFile = splitFile.getAbsolutePath() +"/"+ fileCount +"_"+ file.getName();
                        outputStream = new FileOutputStream(videoFile);
                        System.out.println("File Created Location: "+ videoFile);
                        streamSize = 0;
                    }
                }
                outputStream.write(read);
                streamSize++;
            }
            inputStream.close();
            outputStream.close();
            System.out.println("Total files Split ->"+ totalPartsToSplit);
            } else {
                System.err.println(file.getAbsolutePath() +" File Not Found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
		
    }

    public synchronized void register(BrokerImpl broker, String topic){
        try {
            if(registeredBrokers.contains(broker)){
                System.out.println("Already registered in specified broker");
                out.writeObject("abort");
                out.flush();
            }
            else {
                registeredBrokers.add(broker);
                out.writeObject(this);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	public synchronized void disconnect(BrokerImpl broker, String topic ){
        try {
            if(!registeredBrokers.contains(broker))
                System.out.println("Not subscribed to that broker");
            else{
                connect(broker);
                registeredBrokers.remove(broker);
                out.writeObject(this);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	public synchronized void playData(String video_path) throws IOException {
        File dir = new File(video_path);
        String[] vid_names = dir.list();
        for (String name:vid_names) {
            String video = video_path+name;
            Runtime.getRuntime().exec("wmplayer video");
        }
    }

    
	public void run() {
        Scanner userInput = new Scanner(System.in);

        try {   
            while(true){       
                System.out.println("\nWhat do you want to do?\n");
                System.out.println("1. Subscribe to a channel\n");
                System.out.println("2. Subscribe to a hashtag\n");
                System.out.println("3. Upload Video\n");
                System.out.println("4. Add or remove hashtag to/from a video\n");
                System.out.println("5. Delete a video\n");
                System.out.println("6. View Feed\n");
                System.out.println("7. View hashtags subscribed");
                
                String action = userInput.nextLine();

                if (action.equals("1")){
                    if(brokers.size() == 0){
                        System.out.println("No channels to subscribe to");
                    }
                    else{
                        out.writeObject(action);
                        for (BrokerImpl broker: brokers){
                            for (String publisher : broker.channels) {
                                if (!publisher.equals(channel.channelName))
                                    System.out.println(publisher);
                            }
                        }
                        System.out.println("\nPlease type the name of the channel you want to subscribe to.");
                        String input = userInput.nextLine();

                        boolean alreadySubscribed = false;
                        for(BrokerImpl broker: registeredBrokers){
                            if(broker.hashtags.contains(input)){
                                alreadySubscribed = true;
                                break;
                            }
                        }
                        if(alreadySubscribed){
                            System.out.println("Already registered in specified broker");
                            out.writeObject("abort");
                            out.flush();
                        }
                        else {
                            out.writeObject(input);
                            out.flush();
                
                            try{
                                BrokerImpl answer=(BrokerImpl) in.readObject();
                                if(answer != null){
                                    register(answer, input);
                                    System.out.println("Succesfully Registered to " + input);
                                    out.writeObject(this);
                                    out.flush();
                                }
                                else{
                                    System.out.println("No broker was found for topic " + input);
                                }
                            }
                            catch(ClassNotFoundException e){
                                e.printStackTrace();
                            }
                        }
                    }
                }
                else if(action.equals("2")){
                    if(brokers.isEmpty()){
                        System.out.println("No hashtags to subscribe to");
                    }
                    else{
                        out.writeObject(action);
                        for (BrokerImpl broker: brokers){
                            for (String hashtag : broker.hashtags) {
                                System.out.println(hashtag);
                            }
                        }
                        System.out.println("\nPlease type the hashtag you want to subscribe to.");
                       
                        String input = userInput.nextLine();
                        boolean alreadySubscribed = false;
                        for(BrokerImpl broker: registeredBrokers){
                            if(broker.hashtags.contains(input)){
                                alreadySubscribed = true;
                                break;
                            }
                        }

                        if(alreadySubscribed){
                            System.out.println("Already registered in specified broker");
                            out.writeObject("abort");
                            out.flush();
                        }
                        else{
                            out.writeObject(input);
                            out.flush();
                            try{
                                BrokerImpl answer=(BrokerImpl) in.readObject();

                                if(answer != null){
                                    register(answer, input);
                                    System.out.println("Succesfully Registered to " + input);
                                    out.writeObject(this);
                                    out.flush();
                                }
                                else{
                                    System.out.println("No broker was found for topic " + input);
                                }
                            }
                            catch(ClassNotFoundException e){
                                e.printStackTrace();
                                
                            }
                        }
                       
                    }
                }
                else if (action.equals("3")){
                    out.writeObject(action);
                    out.flush();
                    try {
                        this.brokers = (ArrayList<BrokerImpl>) in.readObject();
                    } catch(ClassNotFoundException e){
                        e.printStackTrace();
                    }
                    System.out.println("Name of the video you want to upload: ");
                    String input=userInput.nextLine();
                    VideoFile new_vid = upload(input); //h upload tha kanei return to neo videofile object
                    System.out.println("Enter the video's hashtags: ");
                    while (!input.isEmpty()) {
                        input=userInput.nextLine();
                        addHashTag(input, new_vid);
                    }
                    updateNodes();
                }
                else if (action.equals("4")){
                    if(channel.videosPublished.isEmpty()){
                        System.out.println("You have no videos!\n");
                        continue;
                    }
                    out.writeObject(action);
                    out.flush();
                    try {
                        this.brokers = (ArrayList<BrokerImpl>) in.readObject();
                    } catch(ClassNotFoundException e){
                        e.printStackTrace();
                    }
                    System.out.println("Your available videos are:");
                    int i = 0;
					for(VideoFile video: channel.videosPublished){
                        System.out.println(i + ". " + video.getVideoName());
                        i++;
                    }
                    System.out.println("");

                    System.out.println("Select the index of the video you want to add or remove a hashtag");
                    int index = userInput.nextInt();
                    userInput.nextLine();

                    VideoFile video = channel.videosPublished.get(index);

                    System.out.println("These are the hashtags for your selected video: ");
                    
                    for(String hashtag: video.getAssociatedHashtags()){
                        System.out.println(hashtag);
                    }
                    System.out.println("Enter the name hashtag to add or remove (If it already exists, it will be removed): ");
                    String hashtag = userInput.nextLine();
                    BrokerImpl respBroker = hashTopic(hashtag);

                    if(channel.videosPublished.get(channel.videosPublished.indexOf(video)).getAssociatedHashtags().contains(hashtag)){
                        removeHashTag(hashtag, video);
                    }
                    else{
                        addHashTag(hashtag, video);
                    }
                    this.updateNodes();
                }
                else if (action.equals("5")) {
                    if(channel.videosPublished.isEmpty()){
                        System.out.println("You have no videos!\n");
                        continue;
                    }
                    out.writeObject(action);
                    out.flush();
                    try {
                        brokers = (ArrayList) in.readObject();
                    } catch(ClassNotFoundException e){
                        e.printStackTrace();
                    }

                    System.out.println("These are your videos:");
                    for(VideoFile video: channel.videosPublished){
                        System.out.println(channel.videosPublished.indexOf(video) + " " + video.getVideoName() );
                    }

                    System.out.println("\nPlease write the number of the video you want to delete: ");
                    int index = userInput.nextInt();
                    VideoFile videoToDelete = channel.videosPublished.get(index);

                    for (VideoFile video : channel.videosPublished) {
                        if (channel.videosPublished.get(index).equals(video.getVideoName())) {
                            for (String hashtag : video.getAssociatedHashtags())
                                removeHashTag(hashtag, video);
                            break;
                        }
                    }

                    channel.videosPublished.remove(index);
                    channel.userVideoFilesMap.remove(videoToDelete);
                    updateNodes();

                }
                else if(action.equals("6")) {
                    for (BrokerImpl broker: registeredBrokers) {
                        Iterator it = broker.registeredUsers.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry<String, AppNode> pair = (Map.Entry)it.next();
                            if (pair.getValue().equals(this)) {
                                String path = broker.pull(pair.getKey());
                                playData(path);
                            }
                        }
                    }
                }
                else if(action.equals("7")){
                    System.out.println("You're subscribed to these hashtags:");
                    for (BrokerImpl broker: registeredBrokers){
                        for (String hashtag: broker.hashtags) {
                            System.out.print(hashtag + " ");
                        }
                    }
                    System.out.println();
                }
                else{
                    System.out.println("Please choose a valid option");
                }    
            }

        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }/* catch (TikaException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }*/
    }
}


