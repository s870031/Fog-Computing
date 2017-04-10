import java.io.Serializable;

@SuppressWarnings("serial")
public class RequestMsg extends Message implements Serializable{
	int sqno;         // Sequence number
	int type;         // Processing time
	int forwardLimit; // Forward limit
	String ipaddr;    // Ip address
	int port;         // Port
	String msg;       // record the orignal message plus the process log in fog node

	int lastNodeID; // record the last hop node ID;

	// Constructor
	public RequestMsg(String msg) {
		String[] tokens = msg.split(" ");
		try{
			this.sqno         = Integer.parseInt(tokens[0].split(":")[1]);
			this.type         = Integer.parseInt(tokens[1].split(":")[1]);
			this.forwardLimit = Integer.parseInt(tokens[2].split(":")[1]);
			this.ipaddr       = tokens[3].split(":")[1];
			this.port         = Integer.parseInt(tokens[4].split(":")[1].trim());	
		}catch(NumberFormatException e) {e.printStackTrace();}

		this.msg = msg;
		this.lastNodeID = -1;
	}
}
