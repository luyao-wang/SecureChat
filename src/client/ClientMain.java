package client;

public class ClientMain {

	/**
	 * Opens a GUI for the chat client.
	 * When valid input is given a connection attempt is done.
	 * Creates a connection using Sockets. If a successful connection 
	 * is initiated a Chat Client is started. If a connection can't be
	 * established the program shows an error message to the user.
	 * 
	 */
	public static void main(String [] args){
		
		new ClientGUI();
		
	}
	
}
