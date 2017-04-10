import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
public class FogProtocol {
	Node node;

	// Constructor
	public void FogProtocol (Node node){
		this.node = node;
	}
	// Define corresponding action about receiving message from IOT node
	public static void receiveIOTMessage(Node node, DatagramSocket serverSocket) {
		byte[] receiveData = new byte[1024];
		byte[] sendData = new byte[1024];

		while(true) {
			Arrays.fill(receiveData, (byte) 0);
			try {
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				String message = new String( receivePacket.getData());
				RequestMsg reqMsg = new RequestMsg(message);

				handleRequestMessage(node, reqMsg); 

			}catch(Exception e){}
		}
	}

	// Define the corrsponding action about receiving message from fog node
	public static void receiveFogMessage(Node node, ObjectInputStream ois) {
		while(true) {
			try{
				Message message = (Message) ois.readObject();
				// Receive State message
				if(message instanceof StateMsg){
					handleStateMessage(node, (StateMsg)message);
				}
				// Receive Request message
				if(message instanceof RequestMsg){
					handleRequestMessage(node, (RequestMsg)message);
				}
			}catch(ClassNotFoundException e){e.printStackTrace();}
			catch(IOException e){}
		}
	}
	// Handle Request Message
	public static synchronized void handleRequestMessage(Node node, RequestMsg message) {
		
		int sqno = message.sqno;
		int type = message.type;
		int forwardLimit = message.forwardLimit;
		String ipaddr = message.ipaddr;	
		int port = message.port;

		//System.out.println(node.nodeID + " RECEIVED: " + message.msg);

		// Serve the message
		int responseTime =  type + getQueueingTime();
		if ( responseTime < node.maxResponseTime) {
			// Put message in queue 
			// and serve message in another thread.
			Main.requestMsgQueue.add(message);
		}
		// Forward the message to cloud or best neighbor
		else {
			if(forwardLimit > 0) {
				if ((node.neighbor.length < 2) && (message.lastNodeID == node.neighbor[0])) {
					// Send to cloud, put message in cloud queue
					Main.cloudQueue.add(message);
					System.out.println(node.nodeID +  " is leave. Send #"+ sqno +" to cloud ");
				}
				else {
					// Forward to best neighbor
					//   choose best neighbor and forward
					int dstID = getBestNeighbor(node, message.lastNodeID);
					message.lastNodeID = node.nodeID;
					message.forwardLimit -= 1;
					message.msg = message.msg + " " + node.nodeID +"("+ responseTime +")"+ ":" + "FORWARDED(" + dstID + ")";
					System.out.println(node.nodeID + " forward message #" + sqno + " to " + dstID);
					forwardRequestMsg(node, dstID, message);
				}
			}
			// Reached forward limit, send to cloud
			else {
				// Send to cloud, put message in cloud queue
				Main.cloudQueue.add(message);
				System.out.println("message #" + sqno + " reached forward limit. " + node.nodeID + " forward to cloud. ");
			}
		}
	}
	// Handle State Message
	public static void handleStateMessage(Node node, StateMsg message) {
		int id = message.nodeID;
		int newQueueingTime = message.queueingTime;
		int i=0;
		// Update the neighbor queueing time
		for (i=0; i<node.neighbor.length; i++) {
			if (node.neighbor[i] == id){
				node.neighborQueueingTime[i] = newQueueingTime;
				break;
			}
		}
		//System.out.println(node.nodeID + " get state message from " + node.neighbor[i] + " : " + node.neighborQueueingTime[i]);
	}

	// Send message to iot node
	public static void sendMsgToIOTNode (String IOTip, int port, String message) {
		InetAddress IPAddress = null;
		try {
			IPAddress = InetAddress.getByName(IOTip);
		}catch (UnknownHostException e){ e.printStackTrace();}
		DatagramSocket clientSocket = null;
		try {
			clientSocket = new DatagramSocket(); // make a Datagram socket
		} catch (SocketException ex) {
			System.out.println("Cannot make Datagram Socket!");
			ex.printStackTrace();
		}

		byte[] sendData = new byte[message.getBytes().length]; // make a Byte array of the data to be sent
		sendData = message.getBytes(); // get the bytes of the message
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); // craft the message to be sent
		try {
			clientSocket.send(sendPacket); // send the message
		} catch (IOException ex) {
			System.out.println("I/O exception happened!");
			ex.printStackTrace();
		}	
	}
	// Send state message
	public static void sendStateMsg (Node node, int dstID, StateMsg message) {
		try {
			ObjectOutputStream oos = node.oStream.get(dstID);
			oos.writeObject(message);
			oos.flush();
		}catch (IOException e) {}
	}
	// Forward message
	public static void forwardRequestMsg (Node node, int dstID, RequestMsg message) {
		try {
			ObjectOutputStream oos = node.oStream.get(dstID);
			oos.writeObject(message);
			oos.flush();
		}catch (IOException e) {e.printStackTrace();}
	}
	// Process message in the queue
	public static void processMessage(Node node) {
		while(true) {
			if(!Main.requestMsgQueue.isEmpty()) {
				RequestMsg message = null;

				// Take the first message in the queue
				message = Main.requestMsgQueue.poll(); // Take an element from queue when it's not empty

				// Get the massage information
				int processTime = 0;
				int sqno = message.sqno;
				int type = message.type;
				int forwardLimit = message.forwardLimit;
				String ipaddr = message.ipaddr;
				int port = message.port;
				processTime = 1000*type;

				// Process the message
				try{Thread.sleep(processTime);} catch(InterruptedException e){e.printStackTrace();};	
				System.out.println(node.nodeID + " served message #" + sqno);
				message.msg = message.msg + " " + node.nodeID + ":" + "SERVED";
				sendMsgToIOTNode(ipaddr,port,message.msg);		
			}
		}
	}
	// Cloud process message in the cloud queue
	public static void cloudProcessMessage(Node node) {
		while(true) {
			if(!Main.cloudQueue.isEmpty()) {
				RequestMsg message = null;

				// Take the first message in the queue
				message = Main.cloudQueue.poll(); // Take an element from queue when it's not empty

				// Get the massage information
				int processTime = 0;
				int sqno = message.sqno;
				int type = message.type;
				int forwardLimit = message.forwardLimit;
				String ipaddr = message.ipaddr;
				int port = message.port;
				processTime = 10*type; // 100 times faster than fog node

				// Process the message
				try{Thread.sleep(processTime);} catch(InterruptedException e){e.printStackTrace();};	
				System.out.println("cloud served message #" + sqno);
				message.msg = message.msg + " CLOUD" + ":" + "SERVED";
				sendMsgToIOTNode(ipaddr,port,message.msg);		
			}
		}
	}
	// Send State message periodically
	// @ param node
	// @ param define period in millisecond
	public static void periodSendStateMsg(Node node, int period) {
		while(true) {
			int queueingTime = FogProtocol.getQueueingTime();
			for (int i=0; i<node.neighbor.length; i++) {
				int id = node.neighbor[i];
				StateMsg message = new StateMsg(node.nodeID, queueingTime);
				sendStateMsg(node,id,message);
			}
			try{Thread.sleep(period);} catch(InterruptedException e){e.printStackTrace();};
		}
	}
	// Calculate the queueing time
	public static int getQueueingTime() {
		Iterator<RequestMsg> itr = Main.requestMsgQueue.iterator();
		int queueingTime = 0;

		// if the queue is empty queueing time is 0
		if(Main.requestMsgQueue.isEmpty()){
			return 0;
		}
		else {
			while(itr.hasNext()){
				RequestMsg message = itr.next();

				// Get the massage processing time
				String[] tokens = message.msg.split(" ");
				int type=0;
				try{
					type         = Integer.parseInt(tokens[1].split(":")[1]);
				}catch(NumberFormatException e) {e.printStackTrace();}
				// Calculate queueing time
				queueingTime += type;
			}
			return queueingTime;
		}
	}
	// Get best neighbor except for last Node
	//   returns -1, if it's leave node
	//   otherwise return best neighbor ID 
	public static int getBestNeighbor(Node node, int lastNodeID) {
		int smallestQueueingTime = Integer.MAX_VALUE;
		int bestNeighborID = -1;

		System.out.print(node.nodeID + " choose best neighbor:");
		for(int i=0; i<node.neighbor.length; i++) {
			System.out.print(" " + node.neighbor[i] + "(" + node.neighborQueueingTime[i] + ")");
			if((node.neighbor[i] != lastNodeID) && 
				node.neighborQueueingTime[i] < smallestQueueingTime) {
				
				smallestQueueingTime = node.neighborQueueingTime[i];
				bestNeighborID = node.neighbor[i];
			}
		}
		System.out.println();
		return bestNeighborID;
	}
}
