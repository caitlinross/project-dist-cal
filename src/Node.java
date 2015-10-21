/**
 * @author Caitlin Ross and Erika Mackin
 *
 * Node object
 */
import java.util.*;
import java.net.*;
import java.io.*;

public class Node {
	private int port;
	private String[] hostNames;
	private int nodeId;
	private int numNodes; 
	private String logName;
	
	// variables that need to be concerned with synchronization
	private Object lock = new Object();
	private int calendars[][][];
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
	// TODO handle appointment conflict
	public Node(int totalNodes, int port, String[] hostNames, int nodeID) {
		this.nodeId = nodeID;
		this.numNodes = totalNodes;
		
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
	
	public void createNewAppointment(ArrayList<Integer> nodes, String name, Day day, int start, int end, String sAMPM, String eAMPM){
		Appointment newAppt = null;
		int startIndex = Appointment.convertTime(start, sAMPM);
		int endIndex = Appointment.convertTime(end, eAMPM);
		
		// check calendar
		boolean timeAvail = true;
		int time = startIndex;
		while(timeAvail && time < endIndex){
			for (Integer node:nodes){
				synchronized(lock){
					if (this.calendars[node][day.ordinal()][time] != 0){
						timeAvail = false;
					}
				}
			}
			time++;
		}
		
		// create appointment object
		if (timeAvail){
			time = startIndex;
			while(time < endIndex){
				for(Integer node:nodes){
					synchronized(lock){
						this.calendars[node][day.ordinal()][time] = 1;
					}
				}
				time++;
			}
			newAppt = new Appointment(name, day, start, end, eAMPM, sAMPM, nodes);
			insert(newAppt);
		}
		
		// appointment involves other nodes besides itself; need to send messages
		if (nodes.size() > 1 && newAppt != null){
			for (Integer node:nodes){
				if (node != this.nodeId){
					System.out.println("Send to node " + node);
					send(newAppt, node);
				}
			}
		}
		
	}
	
	// write an event to the log
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
	
	// save state of system for recovering from crash
	public void saveNodeState(){
		try{
			FileWriter fw = new FileWriter("nodestate.txt", false);  // overwrite each time
			BufferedWriter bw = new BufferedWriter(fw);
			
			// line 1: nodeid, numnodes, port, hostnames, c
			bw.write(this.nodeId + "," + this.numNodes + "," + this.port + ",");
			for (int i = 0; i < hostNames.length; i++){
				bw.write(hostNames[i] + ",");
			}
			bw.write(this.c + "\n");
			
			// then save the 2D calendar array for each node
			for (int i = 0; i < this.calendars.length; i++){
				for (int j = 0; j < this.calendars[i].length; j++){
					for (int k = 0; k < this.calendars[i][j].length; k++){
						bw.write(this.calendars[i][j][k]);
						if (k != this.calendars[i][j].length - 1)
							bw.write(",");
					}
					bw.write("\n");
				}
			}
			
			// save T
			for (int i = 0; i < this.T.length; i++){
				for (int j = 0; j < this.T[i].length; j++){
					bw.write(this.T[i][j]);
					if (j != this.T[i].length - 1){
						bw.write(",");
					}
				}
				bw.write("\n");
			}
			
			// save events in NP, PL, NE, currentAppts in following format:
			// operation, time, nodeID, appt name, day, start, end, sAMPM, eAMPM, participants
			// for days, use ordinals of enums, for AM/PM: AM = 0, PM = 1
			bw.write("NP," + NP.size() + "\n");
			for (EventRecord eR:NP){
				bw.write(eR.getOperation() + "," + eR.getTime() + "," + eR.getNodeId() + "," + eR.getAppointment().getName() + "," + eR.getAppointment().getDay() + ","
						+ eR.getAppointment().getStart() + "," + eR.getAppointment().getEnd() + "," + eR.getAppointment().getsAMPM() + "," + eR.getAppointment().geteAMPM());
				for (int i = 0; i < eR.getAppointment().getParticipants().size(); i++){
					bw.write(eR.getAppointment().getParticipants().get(i));
					if (i != eR.getAppointment().getParticipants().size() - 1)
						bw.write(",");
				}
				bw.write("\n");
			}
			
			bw.write("PL," + PL.size() + "\n");
			for (EventRecord eR:PL){
				bw.write(eR.getOperation() + "," + eR.getTime() + "," + eR.getNodeId() + "," + eR.getAppointment().getName() + "," + eR.getAppointment().getDay() + ","
						+ eR.getAppointment().getStart() + "," + eR.getAppointment().getEnd() + "," + eR.getAppointment().getsAMPM() + "," + eR.getAppointment().geteAMPM());
				for (int i = 0; i < eR.getAppointment().getParticipants().size(); i++){
					bw.write(eR.getAppointment().getParticipants().get(i));
					if (i != eR.getAppointment().getParticipants().size() - 1)
						bw.write(",");
				}
				bw.write("\n");
			}
			
			bw.write("NE," + NE.size() + "\n");
			for (EventRecord eR:NE){
				bw.write(eR.getOperation() + "," + eR.getTime() + "," + eR.getNodeId() + "," + eR.getAppointment().getName() + "," + eR.getAppointment().getDay() + ","
						+ eR.getAppointment().getStart() + "," + eR.getAppointment().getEnd() + "," + eR.getAppointment().getsAMPM() + "," + eR.getAppointment().geteAMPM());
				for (int i = 0; i < eR.getAppointment().getParticipants().size(); i++){
					bw.write(eR.getAppointment().getParticipants().get(i));
					if (i != eR.getAppointment().getParticipants().size() - 1)
						bw.write(",");
				}
				bw.write("\n");
			}
			
			bw.write("current," + currentAppts.size() + "\n");
			for (Appointment appt:currentAppts){
				bw.write(appt.getName() + "," + appt.getDay() + "," + appt.getStart() + "," + appt.getEnd() + "," + appt.getsAMPM() + "," + appt.geteAMPM());
				for (int i = 0; i < appt.getParticipants().size(); i++){
					bw.write(appt.getParticipants().get(i));
					if (i != appt.getParticipants().size() - 1)
						bw.write(",");
				}
				bw.write("\n");
			}
		}
		catch (IOException e){
			e.printStackTrace();
		}
	}
	
	// insert appointment into dictionary
	public void insert(Appointment appt){
		this.c++;
		this.T[this.nodeId][this.nodeId] = c;
		EventRecord eR = new EventRecord("insert", c, nodeId, appt);
		writeToLog(eR);
		synchronized(lock){
			PL.add(eR);
			currentAppts.add(appt);
			saveNodeState();
		}
	}
	
	// delete appointment from dictionary
	public void delete(Appointment appt){
		this.c++;
		this.T[this.nodeId][this.nodeId] = c;
		EventRecord eR = new EventRecord("delete", c, nodeId, appt);
		writeToLog(eR);
		synchronized(lock){
			PL.add(eR);
			currentAppts.remove(appt);
			saveNodeState();
		}
	}
	
	// checks if we know if node k has learned about event e
	public boolean hasRec(int Ti[][], EventRecord eR, int k){		
		return Ti[k][eR.getNodeId()] >= eR.getTime();
	}
	
	// creates NP, then sends <NP, T> to node k
	public void send(Appointment appt, int k){
		// create NP to send
		synchronized(lock){
			for (EventRecord eR:PL){
				if (!hasRec(this.T, eR, k)){
					NP.add(eR);
				}
			}
			saveNodeState();
		}
		
		// now send NP
		try {
			Socket socket = new Socket(hostNames[k], port);
			OutputStream out = socket.getOutputStream();
			ObjectOutputStream objectOutput = new ObjectOutputStream(out);
			synchronized(lock){
				objectOutput.writeObject(NP);
				objectOutput.writeObject(T);
			}
			objectOutput.writeInt(nodeId);
			objectOutput.close();
			out.close();
			socket.close();
		} catch (IOException e) {
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
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
		
		// handle the appointment
		if (NPk != null){
			synchronized(lock){
				// update NE
				for (EventRecord fR:NPk){
					if (!hasRec(T, fR, nodeId)){
						NE.add(fR);
					}
				}
				
				// update the dictionary and calendar and log
				// TODO update calendar and write new events received to log
				//TODONE: update claendar, but aren't we updating PL in the insert() and delete() functions?
				for (EventRecord dR:NE){
					if (dR.getOperation().equals("insert")){
						currentAppts.add(dR.getAppointment());
						//update calendar time slot to 1 for each node in appt list, for each time slot 
						//between start and end indices, for the given day
						//TODO: do we want to include a warning if this time space, for some
						//bizarro reason is already 1/?
						for (Integer id:dR.getAppointment().getParticipants()) {
							for (int j = dR.getAppointment().getStartIndex(); j < dR.getAppointment().getEndIndex(); j++) {
								this.calendars[id][dR.getAppointment().getDay().ordinal()][j] = 1;
							}
						}
				
					}
				}
				for (EventRecord dR:NE){
					if (dR.getOperation().equals("delete") && currentAppts.contains(dR.getAppointment())){
						currentAppts.remove(dR.getAppointment());
						//update calendar
						for (Integer id:dR.getAppointment().getParticipants()) {
							for (int j = dR.getAppointment().getStartIndex(); j < dR.getAppointment().getEndIndex(); j++) {
								this.calendars[id][dR.getAppointment().getDay().ordinal()][j] = 0;
							}
						}
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
				
				// updates to PL
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
				
				saveNodeState();
			}
		}
		
	}


}
