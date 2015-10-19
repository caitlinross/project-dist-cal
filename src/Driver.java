/**
 * @author Caitlin Ross and Erika Mackin
 *
 * Driver to set up a node
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class Driver {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO change these to command line parameters
		int port = 4445;
		int totalNodes = 4;
		String[] hostNames = new String[4];
		InetAddress inetAddr;
		String hostname = "";
		
		try {
			inetAddr = InetAddress.getLocalHost();
			hostname = inetAddr.getHostName();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		
		System.out.println(hostname);
		//set up this node
		final Node node = new Node(totalNodes, port, hostNames);
		
		// set up this nodes serverSocket that continuously listens for other nodes on a new thread
		Runnable listenThread = new Runnable(){
			public synchronized void run() {
				System.out.println("Start listening for other nodes");
				ServerSocket serverSocket;
		        try {
		        	serverSocket = new ServerSocket(port);
		            while (true) {
		            	final Socket client = serverSocket.accept();
		            	Runnable runnable = new Runnable() {
		                    public synchronized void run() {
		                        node.receive(client);
		                    }
		                };
		                new Thread(runnable).start();
		                
		            }
		        } 
		        catch (IOException e) {
					 System.out.println("Exception caught when trying to listen on port " + port);
				    System.out.println(e.getMessage());
					e.printStackTrace();
				}
			}
		};
		new Thread(listenThread).start();
        
        // TODO add sanity checks on input or otherwise improve it
		while(true){
			Scanner in = new Scanner(System.in);
			String name;
			int start;
			int end;
			Day day;
			ArrayList<Integer> participants = new ArrayList<Integer>();
			System.out.println("Please enter an appointment name\n");
			name = in.nextLine();
			System.out.println("Please enter the appointment day\n");
			String tmpDay = in.nextLine();
			if (tmpDay.equals("Sunday"))
				day = Day.SUNDAY;
			else if (in.nextLine().equals("Monday"))
				day = Day.MONDAY;
			else if (in.nextLine().equals("Tuesday"))
				day = Day.TUESDAY;
			else if (in.nextLine().equals("Wednesday"))
				day = Day.WEDNESDAY;
			else if (in.nextLine().equals("Thursday"))
				day = Day.THURSDAY;
			else if (in.nextLine().equals("Friday"))
				day = Day.FRIDAY;
			else
				day = Day.SATURDAY;
			System.out.println("Please enter a start time in 30 minute increments\n");
			start = in.nextInt();
			System.out.println("Please enter an end time in 30 minute increments\n");
			end = in.nextInt();
			System.out.println("Please enter each participant; enter -1 when done\n");
			int tmp = in.nextInt();
			while (tmp != -1){
				participants.add(tmp);
				System.out.println("Enter next participant, or -1 if done\n");
				tmp = in.nextInt();
			}
			
			node.createNewAppointment(participants, name, day, start, end);
		}
	}
	
	
}
