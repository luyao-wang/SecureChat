package server;

public class ServerMain {

	/**
	 * Opens a GUI for the Server.
	 * When valid input is given the server is started. Listening to
	 * and allowing connections on the port that is supplied by the user.
	 * Connections are created using Sockets. If a successful client connection 
	 * is initiated a ServerThread is started. Handling all the communication
	 * with the connected user.
	 */
	public static void main(String [] args){
		
		new ServerGUI();
		
	}
	
}
