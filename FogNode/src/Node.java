import java.io.*;
import java.net.*;
import java.util.*;
public class Node
{
	public int nodeID;             // node ID
	public String hostName;        // node host name
	public int listenPort;         // node socket listen port
	public int UDPPort;            // node UDP listen port
	public int[] neighbor = null;  // node neighbors
	public int maxResponseTime;    // Max Response Time
	public int [] neighborQueueingTime = null; // record current response time
	ConfigReader R = new ConfigReader(Main.ConfigFile);
	HashMap<Integer,Socket> channels = new HashMap<Integer,Socket>();
	HashMap<Integer,ObjectOutputStream> oStream = new HashMap<Integer,ObjectOutputStream>();
	public boolean socketListening = false;

	// Constructor
	public Node(int nodeID){
		this.nodeID = nodeID;
		this.hostName = R.getNodeHostName(nodeID);
		this.listenPort = R.getNodeListenPort(nodeID)[0];
		this.UDPPort = R.getNodeListenPort(nodeID)[1];
		this.neighbor = R.getNodeNeighbor(nodeID);
		this.neighborQueueingTime = new int[neighbor.length];
		this.maxResponseTime = R.getMaxResponseTime(nodeID);
	}
	// Build connection to neighbor
	public void buildFogConnection() {

		for (int i=0; i<neighbor.length; i++) {
			int neighborID = neighbor[i];
			if ((nodeID < neighborID) && !(socketListening)) {
				serverSocketListen();
			}
			else if (nodeID > neighborID) {
				clientSocketConnect(neighborID);
			}
		}
	}

	// Socket Server listen
	public void serverSocketListen() {				
		socketListening = true;
		new ServerThread(this).start();	
	}

	// Keep trying to connect to server socket until succeeded
	// @param destination node ID
	public void clientSocketConnect(int ID) {
		boolean tryConnect = true;
		while(tryConnect){
			//System.out.println (nodeID + " send connection request to " + ID);
			try{
				String hostName = R.getNodeHostName(ID);
				int port = R.getNodeListenPort(ID)[0];
				InetAddress address = InetAddress.getByName(hostName);

				Socket clientSocket = new Socket(address, port);
				ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());

				channels.put(ID, clientSocket);
				oStream.put(ID, oos);
				// Handle input stream in new thread
				new InputStreamThread(this,clientSocket).start(); 

				tryConnect = false;
			} catch (IOException e) {
				try{Thread.sleep(2000);}catch(InterruptedException ie){ie.printStackTrace();}
			}
		}
	}
	public void UDPServerSocketListen() throws Exception{
		DatagramSocket serverSocket = new DatagramSocket(UDPPort);

		FogProtocol.receiveIOTMessage(this, serverSocket);	
	}

	/*	
		public static void main (String args[])
		{
		Node n = new Node(1);
		System.out.println("NodeID: " + n.nodeID );
		System.out.println("status: " + n.status );
		System.out.println("port: " + n.listenPort);
		System.out.print("neighbor #: " + n.neighbor.length + ", ");
		for(int i=0; i<n.neighbor.length; i++)
		System.out.print(n.neighbor[i] + " ");
		System.out.print("\n");

		for(int i=0; i<n.vector.length; i++)
		System.out.print(n.vector[i] + " ");
		}
		*/ 
}
// Server socket listen thread
class ServerThread extends Thread{
	Node node;
	int port; 
	String hostName;  
	int nodeID; 

	public ServerThread(Node node){
		this.node = node;
		this.port = node.listenPort;
		this.hostName = node.hostName;
		this.nodeID  = node.nodeID;
	}	

	public void run(){
		try{
			ServerSocket serverSock = new ServerSocket(port);  // Create a server socket service at port
			//System.out.println( hostName +"(" + nodeID + ")" + " server socket listening...");
			node.socketListening = true;

			while(true){                                       //  Server starts infinite loop waiting for accept client
				Socket sock = serverSock.accept();             //    Wait for client connection
				new ClientThread(sock,node).start();           //    Start new thead to handle client connection
			}
		}catch (IOException ex) {ex.printStackTrace();}
	}
}
// Socket accept connection
//    create new thread for each connection
class ClientThread extends Thread{	
	Socket cSocket; // Client Socket
	Node node;
	ConfigReader R = new ConfigReader(Main.ConfigFile);

	public ClientThread(Socket cSocket, Node node){
		this.cSocket = cSocket;
		this.node = node;
	}
	public void run() {
		String  dstHostName = cSocket.getInetAddress().getHostName().split("\\.")[0];
		int dstID = R.getIDFromHostName(dstHostName);

		System.out.println(node.nodeID + " - " + dstID + " channel created");
		try{
			// Save output Stream
			ObjectOutputStream oos = new ObjectOutputStream(cSocket.getOutputStream());
			node.channels.put(dstID, cSocket);
			node.oStream.put(dstID, oos);
			// Run input stream
			ObjectInputStream ois = new ObjectInputStream(cSocket.getInputStream());
			FogProtocol.receiveFogMessage(node,ois);	
		}catch(IOException e){e.printStackTrace();}
	}
}
// A new thread handle input stream
class InputStreamThread extends Thread{
	Node node;
	Socket clientSocket;

	public InputStreamThread(Node node, Socket  clientSocket) {
		this.node = node;
		this.clientSocket = clientSocket;
	}
	public void run() {
		try{
			ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
			// Run Fog node Protocol
			FogProtocol.receiveFogMessage(node,ois);
		}catch(IOException e){e.printStackTrace();}
	}
}
