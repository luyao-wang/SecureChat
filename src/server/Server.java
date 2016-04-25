package server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;


/**
 * A simple Server implementing Runnable.
 * The server socket accepts new connections and gives each new connection
 * a thread with a refference to the client socket. This socket is used to 
 * initiate an input- and outputstream. The thread listens to input made by the user
 * and sends it to all connected clients outputstream.
 * 
 * @author Tomas
 * @version 1.0
 */
public class Server implements Runnable{
	private static final String localhost = "127.0.0.1";
	static final String SECRET_KEY_ALGO = "AES";
	static final String KEY_PAIR_ALGO = "RSA";
	static PrivateKey privateKey;
	static PublicKey publicKey;
	static SecretKey secretKey;
	static Cipher cipherSecretKey; //used when encrypting/decrypting sealed object to/from client
	
	static DateFormat dateFormat;
	private boolean running; 
	private ServerSocket serverSocket;
	ServerGUI gui;
	
	
	/**
	 * Creates a Server instance. 
	 * @param socket is the server socket.
	 * @param gui is a refference to the ServerGUI that created this Server.
	 * @throws NoSuchAlgorithmException 
	 * @throws NoSuchPaddingException 
	 */
	public Server(ServerSocket serverSocket, ServerGUI gui) throws 
	NoSuchAlgorithmException, NoSuchPaddingException{
		
		KeyPairGenerator kpGenerator = KeyPairGenerator.getInstance(KEY_PAIR_ALGO);
		KeyPair kp = kpGenerator.generateKeyPair();	
		privateKey = kp.getPrivate();
		publicKey = kp.getPublic();
		
		cipherSecretKey = Cipher.getInstance(SECRET_KEY_ALGO);
		KeyGenerator generator = KeyGenerator.getInstance(SECRET_KEY_ALGO);
		generator.init(new SecureRandom());
		secretKey = generator.generateKey();
		
		this.serverSocket = serverSocket;
		this.gui = gui;
		this.running = true;
		dateFormat = new SimpleDateFormat("HH:mm");
	}// constructor end
	
	
	
	
	/**
	 * Sets the user count in the titlebar.
	 * Called by ServerThreads at connect and disconnect.
	 * @param numbUsers is the current number of users connected to the chat.
	 */
	protected void setUserCount(int numbUsers){
		gui.setTitle(gui.activeTitle + numbUsers);
	}
	
	
	/** 
	 * As long as the server is running it's listening for connecting clients.
	 * Each new client is represented as an object of the ServerThread class.
	 * When a connection is made an ServerThread object is created and started.
	 */
	public void run(){
		
		try {
			Socket clientSocket;
			while((clientSocket = serverSocket.accept()) != null && running){
				ServerThread serverThread = new ServerThread(clientSocket, this);
				serverThread.start();
			}
		} catch(SocketException | InterruptedException e){
			e.printStackTrace();
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
	    	e.printStackTrace();
		} catch (IOException e){
	    	e.printStackTrace();
		} finally{
			closeAllResources();
		}
	}// run end
	
	
	/**
	 * Used to end the run method. First setting the boolean flag running to false
	 * and opening a socket that the serverSocket accepts which makes it break
	 * out of the blocking.
	 * The run method then goes to the finally block and closes all connections.
	 * @param port is used to open a socket on the port that the serverSocket uses.
	 */
	void close(int port){
		running = false;
		try {
			Socket socket = new Socket(localhost, port);
			socket.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}// close end
	
	
	
	/**
	 * Informs all clients that the server has been disconnected
	 * and stops each thread - forcing them to close their resources.
	 * @throws IOException 
	 */
	void closeAllResources(){
		try {	
			synchronized(ServerThread.threads){
			    for(ServerThread thread : ServerThread.threads){
					thread.disconnectClient();
					thread.closeResources();
				}
			}
			serverSocket.close();
			System.out.println("SERVER CLOSED NICELY");
		} catch (IOException e) {
			System.err.println("SERVER CLOSED BRUTALLY");
		} 
	}// closeAllResources end

	
	
}// Server end

