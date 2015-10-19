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
	// TODO need to add in node recovery from crash 
	public Node(int totalNodes, int port, String[] hostNames) {
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
		
		this.logName = "appointments.log";
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
		
		// TODO handle appointment conflict
		
	}
	
	public void writeToLog(EventRecord eR){
		try{
			FileWriter fw = new FileWriter(this.logName, true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("------- new event record -------\n");
			bw.write("Operation: " + eR.getOperation() + "\n");
			bw.write("Node clock: " + eR.getTime() + "\n");
			bw.write("Node Id: " + eR.getNodeId() + "\n");
			bw.write("Appointment to be ");
			if (eR.getOperation().equals("delete"))
				bw.write("deleted from ");
			else
				bw.write("added to ");
			bw.write("dictionary\n");
			bw.write("Appointment name: " + eR.getAppointment().getName() + "\n");
			bw.write("Day: " + eR.getAppointment().getDay() + "\n");
			bw.write("Start time: " + eR.getAppointment().getStart() + "\n");
			bw.write("End time: " + eR.getAppointment().getEnd() + "\n");
			bw.write("Participants: ");
			for (Integer node:eR.getAppointment().getParticipants()){
				bw.write(node + " ");
			}
			bw.write("\n");
			bw.close();
		}
		catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public void insert(Appointment appt){
		this.c++;
		this.T[this.nodeId][this.nodeId] = c;
		EventRecord eR = new EventRecord("insert", c, nodeId, appt);
		writeToLog(eR);
		PL.add(eR);
		currentAppts.add(appt);
	}
	
	public void delete(Appointment appt){
		this.c++;
		this.T[this.nodeId][this.nodeId] = c;
		EventRecord eR = new EventRecord("delete", c, nodeId, appt);
		writeToLog(eR);
		PL.add(eR);
		currentAppts.remove(appt);
	}
	
	public boolean hasRec(int Ti[][], EventRecord eR, int k){		
		return Ti[k][eR.getNodeId()] >= eR.getTime();
	}
	
	// creates NP, then sends <NP, T> to node k
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
			objectOutput.writeObject(NP);
			objectOutput.writeObject(T);
			objectOutput.writeObject(nodeId);
			objectOutput.close();
			out.close();
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        
	}
	
	// receives <NP, T> from node k
	public void receive(Socket clientSocket){
		Set<EventRecord> NPk = null;
		int Tk[][] = null;
		int k = -1;

		try {
			// get the objects from the message
			InputStream in = clientSocket.getInputStream();
			ObjectInputStream objectInput = new ObjectInputStream(in);
			NPk = (HashSet<EventRecord>)objectInput.readObject();
			Tk = (int[][])objectInput.readObject();
			k = objectInput.readInt();
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
		
		// handle the appointment
		if (NPk != null){
			// update NE
			for (EventRecord fR:NPk){
				if (!hasRec(T, fR, nodeId)){
					NE.add(fR);
				}
			}
			
			// update the dictionary and calendar
			for (EventRecord dR:NE){
				if (dR.getOperation().equals("insert")){
					currentAppts.add(dR.getAppointment());
					
				}
			}
			for (EventRecord dR:NE){
				if (dR.getOperation().equals("delete") && currentAppts.contains(dR.getAppointment())){
					currentAppts.remove(dR.getAppointment());
				
				}
			}
			
			// update T
			for (int i = 0; i < numNodes; i++){
				T[nodeId][i] = Math.max(T[nodeId][i], Tk[k][i]);
			}
			for (int i = 0; i < numNodes; i++){
				for (int j = 0; j < numNodes; j++){
					T[i][j] = Math.max(T[i][j], Tk[i][j]);
				}
			}
			
			// update PL
			for (EventRecord eR:PL){
				for (int j = 0; j < numNodes; j++){
					if (hasRec(T, eR, j)){
						PL.remove(eR);
					}
				}
			}
			for (EventRecord eR:NE){
				for (int j = 0; j < numNodes; j++){
					if (!hasRec(T, eR, j)){
						PL.add(eR);
					}
				}
			}
			
		}
		
	}


}
