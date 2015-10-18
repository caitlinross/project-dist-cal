/**
 * @author Caitlin Ross and Erika Mackin
 *
 */

import java.io.IOException;
import java.net.*;

public class Driver {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO change these to command line parameters
		int port = 4444;
		int totalNodes = 4;
		
		//set up this node
		Node node = new Node(totalNodes, port);
		//new Thread(node).start();
		
		// set up this nodes serverSocket that continuously listens for other nodes
		ServerSocket serverSocket;
        try {
        	serverSocket = new ServerSocket(port);
            while (true) {
            	Socket client = serverSocket.accept();
            	Runnable runnable = new Runnable() {
                    public synchronized void run() {
                        node.receive(client);
                    }
                };
                new Thread(runnable).start();
                
            }
        } 
        catch (IOException e) {
			// TODO Auto-generated catch block
			 System.out.println("Exception caught when trying to listen on port " + port);
		    System.out.println(e.getMessage());
			e.printStackTrace();
		}
        

	}
	
	
}
