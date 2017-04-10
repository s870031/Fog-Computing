import java.io.*;
import java.nio.charset.Charset;
public class ConfigReader
{
	public String ConfigFile;


	public ConfigReader(String ConfigFile){
		this.ConfigFile = ConfigFile;
	}

	public String getNodeHostName(int nodeID){
		String line;
		String[] token;
		int lineNum = 0;     // File line number counter
		int NumOfNode = 0;    // Number of Node
		String hostName = null;

		// Read Config.txt 
		try(
				InputStream       fis = new FileInputStream(ConfigFile);
				InputStreamReader isr = new InputStreamReader(fis);
				BufferedReader    br = new BufferedReader(isr);
		   )
		{
			while ((line = br.readLine()) != null){
				line = line.replaceAll("#(.*)","");    // Delete comments
				if(line.matches("^\\s*$")) continue;   // Delete empty lines
				line = line.replaceAll("^\\s+","");    // Delete space in the begining

				token = line.split("\\s+");            // Save each entity in token array
				lineNum++;
				if (lineNum == 1)
				{
					NumOfNode = Integer.parseInt(token[0]);   // Get Number of node
					continue;
				}
				else if((lineNum-1) <= NumOfNode && nodeID == Integer.parseInt(token[0]))
				{
					// Get host name of node
					hostName = token[1];  
				}				
			}	
		}
		// Exception
		catch (FileNotFoundException ex) {System.out.println(ex.getMessage()); }
		catch (IOException ioe){System.out.println(ioe.getMessage());}

		return hostName;
	}
	//	
	// Get  Node Socket Port:
	//   input node ID, return node TCP and UDP socket
	//   server listen port
	//
	//   listenPort[0]: TCP port
	//   listenPort[1]: UDP port
	//
	public int [] getNodeListenPort(int nodeID){
		String line;
		String[] token;
		int lineNum = 0;     // File line number counter
		int NumOfNode = 0;    // Number of Node
		int [] listenPort = new int[2];

		// Read Config.txt 
		try(
				InputStream       fis = new FileInputStream(ConfigFile);
				InputStreamReader isr = new InputStreamReader(fis);
				BufferedReader    br = new BufferedReader(isr);
		   )
		{
			while ((line = br.readLine()) != null) {
				line = line.replaceAll("#(.*)","");    // Delete comments
				if(line.matches("^\\s*$")) continue;   // Delete empty lines
				line = line.replaceAll("^\\s+","");    // Delete space in the begining

				token = line.split("\\s+");            // Save each entity in token array
				lineNum++;
				if (lineNum == 1){
					NumOfNode = Integer.parseInt(token[0]);   // Get Number of node
					continue;
				}
				else if((lineNum-1) <= NumOfNode && nodeID == Integer.parseInt(token[0])){
					// Get listern port of node
					listenPort[0] = Integer.parseInt(token[2]);  // Get TCP port  
					listenPort[1] = Integer.parseInt(token[3]);  // Get UDP port
				}				
			}	
		}
		// Exception
		catch (FileNotFoundException ex) {System.out.println(ex.getMessage()); }
		catch (IOException ioe){System.out.println(ioe.getMessage());}

		return listenPort;
	}
	//
	// Get Node Neighbor:
	//    input node ID, return node neighbor 
	//    in an array
	//
	public int [] getNodeNeighbor(int nodeID){
		String line;
		String[] token;
		int lineNum = 0;     // File line number counter
		int NumOfNode = 0;    // Number of Node
		int listenPort = 0;
		int [] neighbor = null;

		// Read Config.txt 
		try(
				InputStream       fis = new FileInputStream(ConfigFile);
				InputStreamReader isr = new InputStreamReader(fis);
				BufferedReader    br = new BufferedReader(isr);
		   )
		{
			while ((line = br.readLine()) != null) {
				line = line.replaceAll("#(.*)","");    // Delete comments
				if(line.matches("^\\s*$")) continue;   // Delete empty lines
				line = line.replaceAll("^\\s+","");    // Delete space in the begining

				token = line.split("\\s+");            // Save each entity in token array
				lineNum++;
				if (lineNum == 1){
					NumOfNode = Integer.parseInt(token[0]);   // Get Number of node
					continue;
				}
				if(lineNum > (NumOfNode+1)){
					if ( nodeID == lineNum-(NumOfNode+1)){
						neighbor = new int[token.length];
						for(int i=0; i<token.length; i++)
							// Get node neighbor
							neighbor[i] = Integer.parseInt(token[i]);
					}
				}
			}	
		}
		// Exception
		catch (FileNotFoundException ex) {System.out.println(ex.getMessage()); }
		catch (IOException ioe) {System.out.println(ioe.getMessage());}

		return neighbor;
	}

	//
	// Get general setting:
	//   return first line of config.txt in an
	//   array
	//
	public int [] getGeneralInfo(){
		String line;
		String token[];
		int generalSetting[] = null;    // gerneral setting array
		int lineNum = 0;
		// Read Config.txt 
		try(
				InputStream       fis = new FileInputStream(ConfigFile);
				InputStreamReader isr = new InputStreamReader(fis);
				BufferedReader    br = new BufferedReader(isr);
		   )
		{
			while ((line = br.readLine()) != null) 
			{
				line = line.replaceAll("#(.*)","");    // Delete comments
				if(line.matches("^\\s*$")) continue;   // Delete empty lines
				line = line.replaceAll("^\\s+","");    // Delete space in the begining

				token = line.split("\\s+");            // Save each entity in token array
				lineNum++;
				if (lineNum == 1)
				{
					// Get general setting from config.txt 1st line
					generalSetting = new int[token.length];
					for (int i=0; i<token.length; i++)
						generalSetting[i] = Integer.parseInt(token[i]);
				}

			}	
		}
		// Exception
		catch (FileNotFoundException ex){System.out.println(ex.getMessage());}
		catch (IOException ioe){System.out.println(ioe.getMessage());}

		return	generalSetting;
	}
	public int [][] getConnectTable(){
		int numOfNode = this.getGeneralInfo()[0];
		int [][] connectTable = new int[numOfNode][numOfNode];

		for(int node=1; node<=numOfNode; node++){
			int [] neighbor;
			neighbor = this.getNodeNeighbor(node);

			for(int n=0; n<neighbor.length; n++){
				connectTable[node][neighbor[n]] = 1;
			}
		}
		return connectTable;
	}
	// TODO this function is ugly delete it later
	public int getIDFromHostName(String hostName){
		int numOfNode = this.getGeneralInfo()[0];

		for(int id=1; id<=numOfNode; id++) {
			if (this.getNodeHostName(id).equals(hostName))
				return id;
		}		

		return -1;
	}
	public int getMaxResponseTime(int nodeID){
		String line;
		String[] token;
		int lineNum = 0;     // File line number counter
		int NumOfNode = 0;    // Number of Node
		int maxResponseTime = 0;

		// Read Config.txt 
		try(
				InputStream       fis = new FileInputStream(ConfigFile);
				InputStreamReader isr = new InputStreamReader(fis);
				BufferedReader    br = new BufferedReader(isr);
		   )
		{
			while ((line = br.readLine()) != null) {
				line = line.replaceAll("#(.*)","");    // Delete comments
				if(line.matches("^\\s*$")) continue;   // Delete empty lines
				line = line.replaceAll("^\\s+","");    // Delete space in the begining

				token = line.split("\\s+");            // Save each entity in token array
				lineNum++;

				if (lineNum == 1){
					NumOfNode = Integer.parseInt(token[0]);   // Get Number of node
					continue;
				}
				else if((lineNum-1) <= NumOfNode && nodeID == Integer.parseInt(token[0])){
					// Get listern port of node
					maxResponseTime = Integer.parseInt(token[4]);  // Get UDP port
				}				
			}	
		}
		// Exception
		catch (FileNotFoundException ex) {System.out.println(ex.getMessage()); }
		catch (IOException ioe){System.out.println(ioe.getMessage());}

		return maxResponseTime;
	}
}
