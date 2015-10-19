/**
 * @author Caitlin Ross and Erika Mackin
 *
 */
import java.util.*;
import java.net.*;
import java.io.*;

public class Node {
	private int port;
	private String[] hostNames;
//	private Thread thread;
//	private ServerSocket serverSocket;
	
	private int nodeId;
	private int calendars[][][];
	private static int numNodes = 0; 
	
	private String logName;
	private Set<EventRecord> PL;
	private Set<EventRecord> NE;
	private Set<EventRecord> NP;
	
	private Set<Appointment> currentAppts;
	private int T[][];
	private int c;
	
	/**
	 * 
	 */
	public Node(int totalNodes, int port, String[] hostNames) {
		// TODO Auto-generated constructor stub
		
		this.nodeId = Node.numNodes;
		Node.numNodes++;
		
		this.calendars = new int[totalNodes][7][48];
		
		this.PL = new HashSet<EventRecord>();
		this.NE = new HashSet<EventRecord>();
		this.NP = new HashSet<EventRecord>();
		
		this.currentAppts = new HashSet<Appointment>();  // dictionary
		this.T = new int[totalNodes][totalNodes];
		this.c = 0;
		
		this.port = port;
		this.hostNames = hostNames;
	}

	/**
	 * @return the nodeId
	 */
	public int getNodeId() {
		return nodeId;
	}
	
	/**
	 * @return the nodeId
	 */
	public int[][][] getCalendars() {
		return calendars;
	}
	
	// TODO: add in write to log
	public void createNewAppointment(ArrayList<Integer> nodes, String name, Day day, int start, int end){
		Appointment newAppt = null;

		// check calendar
		boolean timeAvail = true;
		int time = start;
		while(timeAvail && time < end){
			for (Integer node:nodes){
				if (this.calendars[node][day.ordinal()][time] != 0){
					timeAvail = false;
				}
			}
			time += 30;
		}
		
		// create appointment object
		if (timeAvail){
			time = start;
			while(time < end){
				for(Integer node:nodes){
					this.calendars[node][day.ordinal()][time] = 1;
				}
			}
			newAppt = new Appointment(name, day, start, end, nodes);
			insert(newAppt);
		}
		
		// appointment involves other nodes besides itself; need to send messages
		if (nodes.size() > 1 && newAppt != null){
			for (Integer node:nodes){
				if (node != this.nodeId){
					send(newAppt, node);
				}
			}
		}
		
	}
	
	public void insert(Appointment appt){
		this.c++;
		this.T[this.nodeId][this.nodeId] = c;
		PL.add(new EventRecord("insert", c, nodeId, appt));
		currentAppts.add(appt);
	}
	
	public void delete(Appointment appt){
		this.c++;
		this.T[this.nodeId][this.nodeId] = c;
		PL.add(new EventRecord("delete", c, nodeId, appt));
		currentAppts.remove(appt);
	}
	
	public boolean hasRec(int Ti[][], EventRecord eR, int k){		
		return Ti[k][eR.getNodeId()] >= eR.getTime();
	}
	
	public void send(Appointment appt, int k){
		// create NP to send
		for (EventRecord eR:PL){
			if (!hasRec(this.T, eR, k)){
				NP.add(eR);
			}
		}
		
		// now send NP
		try {
			Socket socket = new Socket(hostNames[k], port);
			OutputStream out = socket.getOutputStream();
			ObjectOutputStream objectOutput = new ObjectOutputStream(out);
			objectOutput.writeObject(appt);
			//BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			objectOutput.close();
			out.close();
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        
	}
	
	public void receive(Socket clientSocket){
		Appointment appt = null;
		try {
			// get the Appointment object from the message
			//PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			InputStream in = clientSocket.getInputStream();
			ObjectInputStream objectInput = new ObjectInputStream(in);
			appt = (Appointment)objectInput.readObject();
			objectInput.close();
			in.close();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		// TODO handle the appointment
		if (appt != null){
			
		}
		
	}


}
