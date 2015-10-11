/**
 * 
 */

/**
 * @author Caitlin Ross and Erika Mackin
 *
 */
import java.util.*;

public class Node {
	private int nodeId;
	private int calendars[][][];
	private static int numNodes = 0; 
	
	private String logName;
	private Set<Appointment> partialLog;
	private Set<Appointment> NE;
	private Set<Appointment> NP;
	
	private Set<Appointment> currentAppts;
	private int T[][];
	private int c;
	/**
	 * 
	 */
	public Node(int totalNodes) {
		// TODO Auto-generated constructor stub
		this.nodeId = Node.numNodes;
		Node.numNodes++;
		
		this.calendars = new int[totalNodes][7][48];
		
		this.partialLog = new HashSet<Appointment>();
		this.NE = new HashSet<Appointment>();
		this.NP = new HashSet<Appointment>();
		
		this.currentAppts = new HashSet<Appointment>();
		this.T = new int[totalNodes][totalNodes];
		this.c = 0;
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
	public void createAppointment(ArrayList<Integer> nodes, String name, Day day, int start, int end){
		this.c++;
		this.T[this.nodeId][this.nodeId] = c;

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
		
		if (timeAvail){
			time = start;
			while(time < end){
				for(Integer node:nodes){
					this.calendars[node][day.ordinal()][time] = 1;
				}
			}
			Appointment newAppt = new Appointment(name, day, start, end, nodes);
			currentAppts.add(newAppt);
			partialLog.add(newAppt);
		}
		
		if (nodes.size() > 1){
			for (Integer node:nodes){
				if (node != this.nodeId){
					send();
				}
			}
		}
		
			
		
		
	}
	
	public void send(){
		
	}

}
