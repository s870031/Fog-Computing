import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {
	// Project General Settings
	public static int nodeID, NumOfNode, maxResponseTime;	
	public static String ConfigFile;
	public static ConcurrentLinkedQueue<RequestMsg> requestMsgQueue // Store request message waiting to be served
					= new ConcurrentLinkedQueue<RequestMsg>();    
	public static ConcurrentLinkedQueue<RequestMsg> cloudQueue      // Store request message waiting to be serverd by cloud
					= new ConcurrentLinkedQueue<RequestMsg>();

	public static void main(String args[]) throws IOException{
		Main.ConfigFile = args[1];                          // Get config file path from args[1]
		ConfigReader R = new ConfigReader(Main.ConfigFile); // Create Reader Object
		int nodeID = Integer.parseInt(args[0]);             // args[0] is nodeID
		Node node = new Node (nodeID);                      // Create a Node	
		Main.NumOfNode = R.getGeneralInfo()[0];

		new IOTMsgThread(node).start();
		new FogMsgThread(node).start();
		new ProcessThread(node).start();
		new CloudThread(node).start();
		try{Thread.sleep(10000);} catch(InterruptedException e){e.printStackTrace();};	
		// TODO make a valuable to check wether the connection has been build
		// but sleep.
		new SendStateMessageThread(node,5000).start();

		//System.out.println(node.nodeID + " max response time: " + node.maxResponseTime);
		/*
		// Node 2 send message to 3
		if(node.nodeID == 2) {
		String msg = "#:0 T:9 FL:4 IP:dc01.utdallas.edu P:5000";
		RequestMsg message = new RequestMsg(msg);

		ObjectOutputStream oos = node.oStream.get(3);
		oos.writeObject(message);
		oos.flush();
		}*/
		
	}
}
// Connect to IOT node and handle the message from
// IOT node.
class IOTMsgThread extends Thread {
	Node node;
	public IOTMsgThread (Node node) {
		this.node = node;	
	}

	public void run() {
		try {
			node.UDPServerSocketListen();
		}catch(Exception e) {}

	}
}
// Connect to Fog node neighbors and handle the message
// from Fog node.
class FogMsgThread extends Thread {
	Node node;
	public FogMsgThread (Node node) {
		this.node = node;
	}

	public void run() {
		node.buildFogConnection();
	}	
}
// Process the request message if available
class ProcessThread extends Thread {
	Node node;
	public ProcessThread (Node node) {
		this.node = node;
	}

	public void run() {
		FogProtocol.processMessage(node);
	}
}
class CloudThread extends Thread {
	Node node;
	public CloudThread (Node node) {
		this.node = node;
	}
	public void run() {
		FogProtocol.cloudProcessMessage(node);
	}
}
// Periodically send state message
class SendStateMessageThread extends Thread {
	Node node;
	int period;
	public SendStateMessageThread (Node node, int period){
		this.node = node;
		this.period = period;
	}
	
	public void run() {
		FogProtocol.periodSendStateMsg(node, period);
	}
}
