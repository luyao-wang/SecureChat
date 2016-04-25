package server;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import message.*;

/**
 * ServerThread keeps track of one connected client. 
 * Each client has an input stream that is attended, every message is sent to 
 * all connected clients listed in the static list 'threads'. 
 * 
 * @author Tomas
 * @version 1.0
 */
class ServerThread extends Thread{
	private static final String SIGNATURE_ALGO = "SHA1withRSA";
	static ArrayList<ServerThread> threads = new ArrayList<ServerThread>();
	
	private String username;
	private Server server;
	private Socket clientSocket;
	private ObjectInputStream inputStream;
	private ObjectOutputStream outputStream;
	private boolean hasClient;
	private boolean hasKeys;
	
	private Signature signature;
	private PublicKey clientPublicKey;
	private Cipher cipherKeyPair; 	//used to decrypt symmetric key recieved from client
	
	private Verifier sender;
	private Verifier reciever;
	
	
	/**
	 * Each instance of this class listens to and sends messages to a client connected to the server. 
	 *
	 * @param socket is the client socket where we listen for and also output messages.
	 * @param server is a refferences to the Server object that created this ServerThread object.
	 * @throws InterruptedException when a thread is occupied and interrupted.
	 * @throws UnsupportedEncodingException is thrown when the char encoding of the outputstream isn't supported.
	 * @throws IOException is thrown when an I/O operation has failed or been interrupted.
	 * @throws NoSuchAlgorithmException if any of the algorithms specified isn't available.
	 * @throws NoSuchPaddingException if any of the used padding mechanisms aren't available.
	 */
	ServerThread(Socket socket, Server server) throws InterruptedException, UnsupportedEncodingException, 
	IOException, NoSuchAlgorithmException, NoSuchPaddingException{
		this.clientSocket = socket;
		this.server = server;
		this.hasClient = true;	//Set to false if a DisconnectMessage is recieved from the client
		this.hasKeys = false;	//Set to true when both keys are recieved from client
		
		cipherKeyPair = Cipher.getInstance(Server.KEY_PAIR_ALGO);
		signature = Signature.getInstance(SIGNATURE_ALGO);
		inputStream = new ObjectInputStream(clientSocket.getInputStream());
		outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
		threads.add(this);
	}// constructor end

	
	/**
	 * Removes client from ArrayList. 
	 * Uses synchronized to put all other calls on hold.
	 * @param client is the client to remove.
	 */
	static void removeClient(ServerThread client){
		synchronized(threads){
			threads.remove(client);
		}
		
	}
	
	
	/**
	 * Closes this object's resources.
	 * @throws IOException if the I/O operation has failed or been interrupted
	 * when trying to close clientSocket/outputStream/inputStream.
	 */
	void closeResources(){
		try{
			if(hasClient && clientSocket != null){ 	//Not disconnected by Client or by closing GUI.
				disconnectClient();					//Probably an exception occured, inform user.
			}
			if(clientSocket != null){
				clientSocket.close();
				outputStream.close();
				inputStream.close();
				clientSocket = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}// closeResources end
	
	
	/**
	 * Save the logged on clients name.
	 * @param name, the name of the client/user.
	 */
	private void setUsername(String name){
		this.username = name;
	}// setUsername end
	
	
	/**
	 * Gets the name of the user.
	 * @return the clients stored username.
	 */
	String getUsername(){
		return username;
	}// getUsername end
	
	
	/**
	 * Go through all the clients and get their names.
	 * Save them to a formatted String, ready to be used
	 * in both server and clients graphical 'user list'-textarea.
	 * @return a formatted String of all connected users.
	 */
	private String getUsernames(){
		String allUsers = "";
		synchronized(threads){
			for(ServerThread st : threads){
				allUsers += " " + st.getUsername() + " \n";
			}
		}
		return allUsers;
	}// getUsernames end
	
	
	/**
	 * Get's the ServerThreads output stream, called when broadcasting messages to all
	 * clients connected to the chat.
	 * @return the clients outputStream.
	 */
	ObjectOutputStream getOutputStream(){
		return outputStream;
	}
	
	
	/**
	 * Listens to client, waiting for messages. 
	 * Sends message to server and all connected clients.
	 * 
	 * First this thread sends the Servers public key to the client.
	 * The client then sends its public key. This thread then encrypts 
	 * the Symmetric-/SecretKey with the clients PublicKey and sends
	 * it to the client.
	 * After that all communication is sent with 
	 * Signed and Sealed objects. 
	 * 
	 * Stopped by client when it sends a DisconnectMessage.
	 * The DisconnectMessage will be stored in a SealedObject
	 * if the key exchange has went well. If the key exchange
	 * somehow failed the client will send an unencrypted DisconnectMessage
	 * To tell the server that it has disconnected.
	 */
	@Override
	public void run(){
		server.setUserCount(threads.size());
		try{
			sendServerPublicKey();
			while(hasClient){
				Object obj = null;
				try{
					obj = inputStream.readObject();				
				} catch(SocketException | EOFException e){	//SocketException if user closes GUI.
					hasClient = false;						//EOFException if client has crashed
				} 											//since it should hace sent an 
															//DisconnectMessage otherwise.
				if(obj instanceof KeyMessage){
					KeyMessage km = (KeyMessage)obj;
					handleKeyFromClient(km);
				}else if(obj instanceof SealedObject){
					SealedObject sealed = (SealedObject) obj;
					handleSealedObject(sealed);
				}else if(obj instanceof DisconnectMessage){ //Used as a last resort for client to communicate 
					hasClient = false;						//with server if keys can't be exchanged
				}
			}// while end
			removeUserAndUpdateGUI();
			System.out.println("CLIENT DISCONNECTED NICELY");
		}catch (IOException e){
			System.err.println("CLIENT DISCONNECTED BRUTALLY");
			e.printStackTrace();
		}catch(ClassNotFoundException | InvalidKeyException | IllegalBlockSizeException e){
			System.err.println("CLIENT DISCONNECTED BRUTALLY");
			e.printStackTrace();
		} catch (BadPaddingException | SignatureException e) {
			System.err.println("CLIENT DISCONNECTED BRUTALLY");
			e.printStackTrace();
		} finally{
			closeResources();
		}
	}// run end
	
	
	/**
	 * 
	 * @param km is the KeyMessage containing the clients PublicKey. 
	 * Saves the key if everything works well. Then sends the servers
	 * Symmetric-/SecretKey.
	 * If the server can't save the key the thread closes its resources.
	 * No communication is allowed to be sent unencrypted.
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws SignatureException
	 * @throws BadPaddingException 
	 */
	private void handleKeyFromClient(KeyMessage km) throws IOException, 
	InvalidKeyException, IllegalBlockSizeException, SignatureException, 
	BadPaddingException{
		boolean recieved = false;
		if(km.getKeytype() == KeyMessage.PUBLIC_KEY){
			if(!recieveClientPublicKey(km)){
				hasClient = false;
			}else{
				recieved = true;
				sendSymmetricKey();
			}
		}
		if(recieved){
			initVerifiers();	 
			sendConnectMessage(km.getUser());
			updateUserListSendUserList();
		}
	}// handleKeyFromServer end
	
	
	/**
	 * When the server has sent its PublicKey, recieved the clients PublicKey and sent the shared
	 * Symmetric/SecretKey successfully - two object are created and used to decrypt and 
	 * encrypt all future messages. The class 'Verifier' exists as both the Client and Server
	 * needs to go through the same steps when decrypting and encrypting messages. 
	 */
	private void initVerifiers(){
		sender = new Verifier(Server.secretKey, Server.cipherSecretKey, Server.publicKey, Server.privateKey, signature);
		reciever = new Verifier(Server.secretKey, Server.cipherSecretKey, clientPublicKey, Server.privateKey, signature);
		hasKeys = true;
	}// initVerifiers end
	
	
	
	/**
	 * Decrypts the SealedObject, retrieves the SignedObject, verifies its signature
	 * and determines the type of message being sent. If its a 'DisconnectMessage'
	 * the Thread shuts down its resources. If it's a 'Message' the content is displayed on 
	 * the server and then encrypted before it's sent to all other clients. 
	 * @param sealed is the SealedObject that's being encrypted.
	 * @throws InvalidKeyException
	 * @throws SignatureException
	 * @throws ClassNotFoundException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws IOException
	 */
	private void handleSealedObject(SealedObject sealed) throws InvalidKeyException, 
	SignatureException, ClassNotFoundException, IllegalBlockSizeException, BadPaddingException, IOException{
		SignedObject sign = reciever.convertSealedObject(sealed);
		if(reciever.validateSignedObject(sign)){
			Object obj = reciever.convertSignedObject(sign);
			if(obj instanceof DisconnectMessage){
				hasClient = false;
			}else if(obj instanceof Message){
				Message m = (Message) obj;
				sign = sender.createSignedObject(m);
				sealed = sender.createSealedObject(sign);
				broadcast(m, sealed);
			}
		}else{
			System.err.println("Server recieved a SignedObject with an invalid signature.");
		}
	}// handleSealedObject end
	
	
	/**
	 * First saves the users name and then broadcasts a successful connection message. 
	 * @throws InvalidKeyException
	 * @throws SignatureException
	 * @throws IOException
	 * @throws IllegalBlockSizeException
	 */
	private void sendConnectMessage(String username) throws InvalidKeyException, 
	SignatureException, IOException, IllegalBlockSizeException{
		setUsername(username);
		String connectMessage = username + " CONNECTED " + Server.dateFormat.format(new Date());
		Message m = new Message(null, connectMessage);
		SignedObject sign = sender.createSignedObject(m);
		SealedObject sealed = sender.createSealedObject(sign);
		broadcast(m, sealed);
	}// sendConnectMessage end
	
	
	/**
	 * Updates the servers userlist and sends it to all connected clients.
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws SignatureException
	 */
	private void updateUserListSendUserList() throws IOException, 
	InvalidKeyException, IllegalBlockSizeException, SignatureException{
		String allUsers = getUsernames();
		server.gui.usersArea.setText(allUsers); 
		SignedObject signed = sender.createSignedObject(new UserListMessage(allUsers));
		SealedObject sealed = sender.createSealedObject(signed);
		broadcast(sealed);
	}// setNameAndSendUserListMessage end
	
	
	/**
	 * Removes the thread from the static list 'threads'.
	 * Shows a DisconnectMessage on the Server and sends it to all connected users.
	 * @throws InvalidKeyException
	 * @throws SignatureException
	 * @throws IOException
	 * @throws IllegalBlockSizeException
	 */
	private void removeUserAndUpdateGUI() throws InvalidKeyException, SignatureException, 
	IOException, IllegalBlockSizeException{ 
		removeClient(this);
		
		String disconnectMessage = username + " DISCONNECTED " + Server.dateFormat.format(new Date());
		Message m = new Message(null,disconnectMessage);
		if(hasKeys){
			SignedObject signed = sender.createSignedObject(m);
			SealedObject sealed = sender.createSealedObject(signed);
			broadcast(m, sealed);
			signed = sender.createSignedObject(new UserListMessage(getUsernames()));
			sealed = sender.createSealedObject(signed);
			broadcast(sealed);
		}else{
			appendToServerWindow(m);
		}
		
		server.gui.usersArea.setText(getUsernames()); 
		server.setUserCount(threads.size());
	}// removeUserAndUpdateGUI end
	
	
	/**
	 * Sends a DisconnectMessage to the client. This makes the client close its 
	 * resources. The message is sent both Signed and Sealed if keys have been
	 * exchanged successfully. Otherwise a plain DisconnectMessage is sent.
	 * @throws InvalidKeyException
	 * @throws SignatureException
	 * @throws IllegalBlockSizeException
	 */
	void disconnectClient(){
		hasClient = false;
		try{
			if(hasKeys){
				SignedObject signed = sender.createSignedObject(new Message(null, "SERVER HAS DISCONNECTED"));
				SealedObject sealed = sender.createSealedObject(signed);
				outputStream.writeObject(sealed);	
				outputStream.flush();
				SignedObject signedDisconnect = sender.createSignedObject(new DisconnectMessage());
				SealedObject sealedDisconnect = sender.createSealedObject(signedDisconnect);
				outputStream.writeObject(sealedDisconnect);	
				outputStream.flush();
			}else{
				outputStream.writeObject(new DisconnectMessage());	
				outputStream.flush();
			}
		} catch(SignatureException | IllegalBlockSizeException e){
			e.printStackTrace();
		} catch(InvalidKeyException | IOException e){
			e.printStackTrace();
		}
	}// disconnectClient end

	
	/**
	 * Appends message to the servers main window. Called by method 'broadcast'.
	 * @param m is the Message to be appended.
	 */
	private void appendToServerWindow(Message m){
		if(m.getUser() == null){//Null if its from server
			server.gui.outputArea.append(m.getMessage() + "\n");
		}else{
			server.gui.outputArea.append(m.getUser() + ": " + m.getMessage() + "\n");
		}
	}// appendToServerWindow end
	
	
	/**
	 * Both shows the Message on the servers main window and sends it as a Sealed Object
	 * to the other clients.
	 * @param m is the unencrypted Message that is used to display content on the
	 * servers main window. 
	 * @param sealed is the object that is sent to the other clients.
	 * @throws IOException
	 */
	private void broadcast(Message m, SealedObject sealed) throws IOException{
		appendToServerWindow(m);
	    synchronized(threads){
		    for(ServerThread client : threads){
		    	client.getOutputStream().writeObject(sealed);
				client.getOutputStream().flush();
			}
	    }
	}// broadcast end
	
	
	/**
	 * Used to broadcast UserListMessages.
	 * @param sealed is a SealedObject containing a UserList.
	 * @throws IOException
	 */
	private void broadcast(SealedObject sealed) throws IOException{
	    synchronized(threads){
		    for(ServerThread client : threads){
		    	client.getOutputStream().writeObject(sealed);
				client.getOutputStream().flush();
			}
	    }
	}// broadcast end
	
	
	/**
	 * Sends the servers PublicKey to its client.
	 * Does this by turning the public key into a byte array that is converted into 
	 * a String base64 and sent in a KeyMessage object.
	 * @throws IOException
	 */
	private void sendServerPublicKey() throws IOException{
		synchronized(threads){
			String keyText = Base64.getEncoder().encodeToString(Server.publicKey.getEncoded());
			outputStream.writeObject(new KeyMessage("SERVER", keyText, KeyMessage.PUBLIC_KEY));
			outputStream.flush();
			System.out.println("SERVER SENDS ITS PUBLIC KEY");
		}
	}// sendPublicKey end
	
	
	/**
	 * Sends the servers Symmetric-/SecretKey to the client. 
	 * Encrypts the keys byte array using the clients PublicKey.
	 * Then encodes it into a base64 String storing it in a
	 * KeyMessage object and sending it to the user.
	 * @throws InvalidKeyException
	 * @throws IOException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	private void sendSymmetricKey() throws InvalidKeyException, IOException, 
	IllegalBlockSizeException, BadPaddingException{
		cipherKeyPair.init(Cipher.ENCRYPT_MODE, clientPublicKey);            
		byte[] keyBytes = cipherKeyPair.doFinal(Server.secretKey.getEncoded());            
		String keyText = new String(Base64.getEncoder().encode(keyBytes));
		outputStream.writeObject(new KeyMessage(null, keyText, KeyMessage.SECRET_KEY)); 
		outputStream.flush();
		System.out.println("SERVER SENDS THE SYMMETRIC KEY");
	}// sendSymmetricKey end
	
	
	
	/**
	 * Decodes the base64 String sent by the client, turning it into a byte array.
	 * The array is then used to generate the clients PublicKey.
	 * @param km is the KeyMessage sent by the client.
	 * @return true if the server can retrieve the key, false otherwise.
	 */
	private boolean recieveClientPublicKey(KeyMessage km){
		try {
			byte[] publicKeyBytes = Base64.getDecoder().decode(km.getKey());
			KeyFactory keyFactory = KeyFactory.getInstance(Server.KEY_PAIR_ALGO);
			EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
			clientPublicKey = keyFactory.generatePublic(publicKeySpec);
			System.out.println("SERVER SAVES CLIENTS PUBLIC KEY");
			return true;
		} catch (NoSuchAlgorithmException | IllegalArgumentException | InvalidKeySpecException e) {
			e.printStackTrace();
		}
		return false;
	}// recieveClientPublicKey end
	
	
		
}// ServerThread end


