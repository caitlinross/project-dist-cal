/**
 * @author Caitlin Ross and Erika Mackin
 *
 */
import java.util.*;
import java.net.*;
import java.io.*;

public class Node {
	private int port;
	private String hostName;
	private Thread thread;
	private ServerSocket serverSocket;
	//private Socket clientSocket;
	private PrintWriter out;
	private BufferedReader in;
	private BufferedReader stdIn;
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
	public Node(int totalNodes, int port) {
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
		
		this.port = port;
		
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
		
		// appointment involves other nodes besides it's self; need to send messages
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
	
	public void receive(Socket clientSocket){
		try {
			this.out = new PrintWriter(clientSocket.getOutputStream(), true);
			this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			//this.stdIn = new BufferedReader(new InputStreamReader(System.in));
			String inputLine;
            while ((inputLine = in.readLine()) != null) {
                out.println(inputLine);
            }
		} 
		catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			 System.out.println("Exception caught when trying to listen on port "
		                + port + " or listening for a connection");
		    System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
	}


}
