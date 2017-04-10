import java.io.Serializable;

@SuppressWarnings("serial")
public class StateMsg extends Message implements Serializable{
	int nodeID;
	int queueingTime;

	public StateMsg(int nodeID, int queueingTime) {
		this.nodeID = nodeID;
		this.queueingTime = queueingTime;
	}
}
