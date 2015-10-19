/**
 * @author Caitlin Ross and Erika Mackin
 *
 * Driver to set up a node
 */

import java.io.*;
import java.net.*;

public class Driver {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO change these to command line parameters
		int port = 4444;
		int totalNodes = 4;
		String[] hostNames = new String[4];
		
		//set up this node
		final Node node = new Node(totalNodes, port, hostNames);
		//new Thread(node).start();
		
		// TODO have a thread running for the serverSocket to listen for other nodes
		// set up this nodes serverSocket that continuously listens for other nodes
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
        
        // TODO get user command line input for creating appointments

	}
	
	
}
