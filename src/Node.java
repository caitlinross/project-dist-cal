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
	private String stateLog;
	
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
	// TODO handle appointment conflict
	/**
	 * @param totalNodes
	 * @param port
	 * @param hostNames
	 * @param nodeID
	 * @param recovery
	 */
	public Node(int totalNodes, int port, String[] hostNames, int nodeID, boolean recovery) {
		this.logName = "appointments.log";
		this.stateLog = "nodestate.txt";
		this.nodeId = nodeID;
		this.numNodes = totalNodes;
		this.port = port;
		this.hostNames = hostNames;
		
		this.calendars = new int[totalNodes][7][48];
		this.PL = new HashSet<EventRecord>();
		this.NE = new HashSet<EventRecord>();
		this.NP = new HashSet<EventRecord>();
		this.currentAppts = new HashSet<Appointment>();  // dictionary
		this.T = new int[totalNodes][totalNodes];
		this.c = 0;
		
		// recover node state if this is restarting from crash
		if (recovery)
			restoreNodeState();
		
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
			
			// line 1: c
			synchronized(lock){
				bw.write(this.c + "\n");
			}
			
			// then save the 2D calendar array for each node
			synchronized(lock){
				for (int i = 0; i < this.calendars.length; i++){
					for (int j = 0; j < this.calendars[i].length; j++){
						for (int k = 0; k < this.calendars[i][j].length; k++){
							bw.write(Integer.toString(this.calendars[i][j][k]));
							if (k != this.calendars[i][j].length - 1)
								bw.write(",");
						}
						bw.write("\n");
					}
				}
			}
			
			// save T
			synchronized(lock){
				for (int i = 0; i < this.T.length; i++){
					for (int j = 0; j < this.T[i].length; j++){
						bw.write(Integer.toString(this.T[i][j]));
						if (j != this.T[i].length - 1){
							bw.write(",");
						}
					}
					bw.write("\n");
				}
			}
			
			// save events in NP, PL, NE, currentAppts in following format:
			// operation, time, nodeID, appt name, day, start, end, sAMPM, eAMPM, participants
			// for days, use ordinals of enums,
			synchronized(lock){
				bw.write("NP," + NP.size() + "\n");
				for (EventRecord eR:NP){
					bw.write(eR.getOperation() + "," + eR.getTime() + "," + eR.getNodeId() + "," + eR.getAppointment().getName() + "," + eR.getAppointment().getDay().ordinal() + ","
							+ eR.getAppointment().getStart() + "," + eR.getAppointment().getEnd() + "," + eR.getAppointment().getsAMPM() + "," + eR.getAppointment().geteAMPM() + ",");
					for (int i = 0; i < eR.getAppointment().getParticipants().size(); i++){
						bw.write(Integer.toString(eR.getAppointment().getParticipants().get(i)));
						if (i != eR.getAppointment().getParticipants().size() - 1)
							bw.write(",");
					}
					bw.write("\n");
				}
				
				bw.write("PL," + PL.size() + "\n");
				for (EventRecord eR:PL){
					bw.write(eR.getOperation() + "," + eR.getTime() + "," + eR.getNodeId() + "," + eR.getAppointment().getName() + "," + eR.getAppointment().getDay().ordinal() + ","
							+ eR.getAppointment().getStart() + "," + eR.getAppointment().getEnd() + "," + eR.getAppointment().getsAMPM() + "," + eR.getAppointment().geteAMPM() + ",");
					for (int i = 0; i < eR.getAppointment().getParticipants().size(); i++){
						bw.write(Integer.toString(eR.getAppointment().getParticipants().get(i)));
						if (i != eR.getAppointment().getParticipants().size() - 1)
							bw.write(",");
					}
					bw.write("\n");
				}
				
				bw.write("NE," + NE.size() + "\n");
				for (EventRecord eR:NE){
					bw.write(eR.getOperation() + "," + eR.getTime() + "," + eR.getNodeId() + "," + eR.getAppointment().getName() + "," + eR.getAppointment().getDay().ordinal() + ","
							+ eR.getAppointment().getStart() + "," + eR.getAppointment().getEnd() + "," + eR.getAppointment().getsAMPM() + "," + eR.getAppointment().geteAMPM() + ",");
					for (int i = 0; i < eR.getAppointment().getParticipants().size(); i++){
						bw.write(Integer.toString(eR.getAppointment().getParticipants().get(i)));
						if (i != eR.getAppointment().getParticipants().size() - 1)
							bw.write(",");
					}
					bw.write("\n");
				}
				
				bw.write("current," + currentAppts.size() + "\n");
				for (Appointment appt:currentAppts){
					bw.write(appt.getName() + "," + appt.getDay().ordinal() + "," + appt.getStart() + "," + appt.getEnd() + "," + appt.getsAMPM() + "," + appt.geteAMPM() + ",");
					for (int i = 0; i < appt.getParticipants().size(); i++){
						bw.write(Integer.toString(appt.getParticipants().get(i)));
						if (i != appt.getParticipants().size() - 1)
							bw.write(",");
					}
					bw.write("\n");
				}
			}
			bw.close();
		}
		catch (IOException e){
			e.printStackTrace();
		}
	}
	
	// recover from node failure
	public void restoreNodeState(){
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(this.stateLog));
			String text = null;
			int lineNo = 0;
			int cal = 0;
			int index = 0;
			int tLimit = 7*numNodes + numNodes;
			int npLimit = 0, plLimit = 0, neLimit = 0, apptLimit = 0;
			int numNP = 0, numNE = 0, numPL = 0, numAppt = 0;
		    while ((text = reader.readLine()) != null) {
		    	String[] parts = text.split(",");
		        if (lineNo == 0){ // restore node clock
		        	this.c = Integer.parseInt(parts[0]);
		        }
		        else if (lineNo > 0 && lineNo <= 7*numNodes ){ // restore calendar
		        		int len = parts.length;
			        	for (int j = 0; j < len; j++){
			        		this.calendars[cal][index][j] = Integer.parseInt(parts[j]);
			        	}
		        	index++;
		        	if (lineNo % 7 == 0){// time to go to next node's calendar
		        		cal++;
		        		index = 0;
		        	}
		        }
		        else if (lineNo > 7*numNodes && lineNo <= tLimit){ // restore T
		        	for (int i = 0; i < this.T.length; i++){
		        		T[index][i] = Integer.parseInt(parts[i]);
		        	}
		        	index++;
		        }
		        else if (lineNo == tLimit + 1){ 
		        	numNP = Integer.parseInt(parts[1]);
		        	npLimit = lineNo + numNP;
		        }
		        else if (lineNo > tLimit + 1 && lineNo <= npLimit && numNP > 0){ // Restore NP's hashset
		        	ArrayList<Integer> list = new ArrayList<Integer>();
		        	for (int i = 9; i < parts.length; i++)
		        		list.add(Integer.parseInt(parts[i]));
		        	Appointment appt = new Appointment(parts[3], Day.values()[Integer.parseInt(parts[4])], Integer.parseInt(parts[5]), Integer.parseInt(parts[6]), 
		        			parts[7], parts[8], list);
		        	EventRecord eR = new EventRecord(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), appt);
		        	NP.add(eR);
		        	
		        }
		        else if (lineNo == npLimit + 1){
		        	numPL = Integer.parseInt(parts[1]);
		        	plLimit = lineNo + numPL;
		        }
		        else if (lineNo > npLimit + 1 && lineNo <= plLimit && numPL > 0){
		        	ArrayList<Integer> list = new ArrayList<Integer>();
		        	for (int i = 9; i < parts.length; i++)
		        		list.add(Integer.parseInt(parts[i]));
		        	Appointment appt = new Appointment(parts[3], Day.values()[Integer.parseInt(parts[4])], Integer.parseInt(parts[5]), Integer.parseInt(parts[6]), 
		        			parts[7], parts[8], list);
		        	EventRecord eR = new EventRecord(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), appt);
		        	PL.add(eR);
		        }
		        else if (lineNo == plLimit + 1){
		        	numNE = Integer.parseInt(parts[1]);
		        	neLimit = lineNo + numNE;
		        }
		        else if (lineNo > plLimit + 1 && lineNo <=  neLimit && numNE > 0){
		        	ArrayList<Integer> list = new ArrayList<Integer>();
		        	for (int i = 9; i < parts.length; i++)
		        		list.add(Integer.parseInt(parts[i]));
		        	Appointment appt = new Appointment(parts[3], Day.values()[Integer.parseInt(parts[4])], Integer.parseInt(parts[5]), Integer.parseInt(parts[6]), 
		        			parts[7], parts[8], list);
		        	EventRecord eR = new EventRecord(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), appt);
		        	NE.add(eR);
		        }
		        else if (lineNo == neLimit + 1){
		        	numAppt = Integer.parseInt(parts[1]);
		        	apptLimit = lineNo + numAppt;
		        }
		        else if (lineNo > neLimit + 1 && lineNo <= apptLimit && numAppt > 0){
		        	ArrayList<Integer> list = new ArrayList<Integer>();
		        	for (int i = 6; i < parts.length; i++)
		        		list.add(Integer.parseInt(parts[i]));
		        	Appointment appt = new Appointment(parts[0], Day.values()[Integer.parseInt(parts[1])], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), 
		        			parts[4], parts[5], list);
		        	currentAppts.add(appt);
		        }
		        lineNo++;
		    }
		    reader.close();
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		} catch (IOException e) {
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
	@SuppressWarnings("unchecked")
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
				/*for (EventRecord dR:NE){
					if (dR.getOperation().equals("insert")){
						currentAppts.add(dR.getAppointment());
						//update calendar time slot to 1 for each node in appt list, for each time slot 
						//between start and end indices, for the given day
						for (Integer id:dR.getAppointment().getParticipants()) {
							for (int j = dR.getAppointment().getStartIndex(); j < dR.getAppointment().getEndIndex(); j++) {
								if (this.calendars[id][dR.getAppointment().getDay().ordinal()][j] == 1)
									System.out.println("You just scheduled over an existing appt");
								this.calendars[id][dR.getAppointment().getDay().ordinal()][j] = 1;
							}
						}
						writeToLog(dR);
				
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
						writeToLog(dR);
					}
				}*/
				// check for appts in currentAppts that need to be deleted
				for (Appointment appt:currentAppts){
					EventRecord dR = containsAppointment(NE, appt);
					if (dR != null && dR.getOperation().equals("delete")){
						currentAppts.remove(appt);
						// update calendar
						for (Integer id:dR.getAppointment().getParticipants()) {
							for (int j = dR.getAppointment().getStartIndex(); j < dR.getAppointment().getEndIndex(); j++) {
								this.calendars[id][dR.getAppointment().getDay().ordinal()][j] = 0;
							}
						}
						writeToLog(dR);
					}
				}
				// check for events in NE that need to be inserted into currentAppts
				for (EventRecord eR:NE){
					if (eR.getOperation().equals("insert")){
						writeToLog(eR);
						EventRecord dR = containsAppointment(NE, eR.getAppointment());
						if (dR == null){ // there's no 'delete()' for this appointment so add to currentAppts
							currentAppts.add(eR.getAppointment());
							//update calendar time slot to 1 for each node in appt list, for each time slot 
							//between start and end indices, for the given day
							for (Integer id:eR.getAppointment().getParticipants()) {
								for (int j = eR.getAppointment().getStartIndex(); j < eR.getAppointment().getEndIndex(); j++) {
									if (this.calendars[id][eR.getAppointment().getDay().ordinal()][j] == 1)
										System.out.println("You just scheduled over an existing appt");
									this.calendars[id][eR.getAppointment().getDay().ordinal()][j] = 1;
								}
							}
							
						}
						else {  // received an insert() and delete() for same appointment
							writeToLog(dR);
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
	
	// determine if a given appointment is the same as one in an EventRecord
	// useful when trying to find the insert or delete of a given appointment
	public static EventRecord containsAppointment(Set<EventRecord> set, Appointment appt){
		for (EventRecord eR:set){
			if (eR.getAppointment().equals(appt) && eR.getOperation().equals("delete"))
				return eR;
		}
		return null;
	}


}
