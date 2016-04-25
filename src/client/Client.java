package client;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import message.*;


/**
 * A simple GUI Chat client that connects to a server. 
 * Host, Port and Username can be give by the user when the program
 * is started. If no arguments are given 
 * the client default to Host: 127.0.0.1 and Port: 2000.
 *    
 * @author Tomas
 * @version 1.0
 */
class Client extends Thread{
	
	private static final String KEY_PAIR_ALGO = "RSA";
	private static final String SIGNATURE_ALGO = "SHA1withRSA";
	private static final String SECRET_KEY_ALGO = "AES";
	
	Socket socket;
	private ObjectOutputStream outputStream;
	private ObjectInputStream inputStream;
	
	private PrivateKey clientPrivateKey;
	private PublicKey clientPublicKey;
	private PublicKey serverPublicKey;
	private SecretKey secretKey;
	private Signature signature;
	private Cipher cipherKeyPair; 	//used when sending symmetric key to server
	private Cipher cipherSecretKey; //used when encrypting/decrypting sealed object to/from server
	
	private Verifier sender;
	private Verifier reciever;
	
	private ClientGUI gui;
	private String user;
	private boolean hasKeys;
	boolean hasServer;
	
	
	/**
	 *
	 * Creates clients Public- and PrivateKey, the PublicKey will be sent to the 
	 * server. Ciphers are created with the same algorithms that are used by the server.  
	 * Saves the name of the user that was entered into the 
	 * ClientGUI. Sets the boolean flag hasServer to true since it's used as a flag 
	 * to keep the client listening to the server. Sets the boolean hasKeys to false,
	 * this will be set to true if the client recieves the Servers PublicKey and 
	 * Symmetric-/SecretKey correctly.
	 * Initiates a ObjectOutputStream and ObjectInputStream with the
	 * help of the Socket parameter.
	 * @param user is the username that the client has entered.
	 * @param gui is a refference to the server GUI.
	 * @param socket is the socket that is used for communication with server.
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	Client(String user, ClientGUI gui, Socket socket) throws 
	UnsupportedEncodingException, IOException, NoSuchAlgorithmException, NoSuchPaddingException{
		this.user = user;
		this.hasServer = true;
		this.hasKeys = false;
	    this.gui = gui;
	    
	    KeyPairGenerator kpGenerator = KeyPairGenerator.getInstance(KEY_PAIR_ALGO);
	    cipherKeyPair = Cipher.getInstance(KEY_PAIR_ALGO);
	    cipherSecretKey = Cipher.getInstance(SECRET_KEY_ALGO);
		KeyPair kp = kpGenerator.generateKeyPair();		
		clientPrivateKey = kp.getPrivate();
		clientPublicKey = kp.getPublic();
		signature = Signature.getInstance(SIGNATURE_ALGO);
		
	    this.socket = socket;
	    this.outputStream = new ObjectOutputStream(socket.getOutputStream());
	    this.inputStream = new ObjectInputStream(socket.getInputStream());
	    
	}// constructor end
	
	
	/**
	 * Sends a Signed and Sealed Message to the server.
	 * Called from the GUI when the user has entered text.
	 * @param text is the content entered by the user.
	 */
	void sendMessage(String text){
		try {
			SignedObject signed = sender.createSignedObject(new Message(user, text));
			SealedObject sealed = sender.createSealedObject(signed);
			outputStream.writeObject(sealed);
			outputStream.flush();
		} catch (InvalidKeyException | SignatureException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException | IOException e) {
			e.printStackTrace();
		} 
	}// sendMessage end
	
	
	/**
	 * Disable GUI, making it impossible for the user to enter 
	 * new input. 
	 * If the server hasn't told the client to disconnect
	 * the client informs the server that it's leaving.
	 * Closes Socket, BufferedReader, PrintWriter.
	 */
	void closeResources(){
		gui.disableUserInterface();
		if(hasServer && socket != null){	//Not disconnected by GUI or by server
			disconnectServer();				//try to inform server of disconnect
		}
		if(socket != null){
			try {
				outputStream.close();
				inputStream.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			socket = null;
		}
	}// closeResources end
	

	/**
	 * A loop retrieving all input sent from the server.
	 * We catch SocketException in the readObject method 
	 * as it's thrown when the resources are closed. EOFException
	 * tells us that the server is gone, therefore this exception is also 
	 * caught, making the client close its resources and exit.
	 * KeyMessages are the first ones sent between client and server.
	 * The Client recives the servers PublicKey. If the keys have been 
	 * exchanged correctly all other messages are Signed and Sealed, both
	 * by client and server. If keys can't be exchanged correctly, 
	 * a DisconnectMessage is sent as a last resort to tell the Server that it 
	 * should exit. Sealed object are sent to the 'handleSealedObject' method.
	 */
	public void run(){
		try {
			while(hasServer){
				Object obj = null;
				try{
					obj = inputStream.readObject();
				}catch(SocketException | EOFException e){
					hasServer = false;
				}
				if(obj instanceof KeyMessage){		
					KeyMessage m = (KeyMessage)obj;
					handleKeysFromServer(m);
				}
				else if(obj instanceof SealedObject){
					SealedObject sealed = (SealedObject)obj;
					handleSealedObject(sealed);
				}else if(obj instanceof DisconnectMessage){ 	//Used as a last resort for client to communicate 
					hasServer = false;							//with server if keys can't be exchanged
				}
			}// while end
		} catch (ClassNotFoundException | InvalidKeyException e) {
			e.printStackTrace();
			appendMessageToClientWindow(new Message(null,"AN ERROR OCCURED, EXITING..."));
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
			appendMessageToClientWindow(new Message(null,"AN ERROR OCCURED, EXITING..."));
		} catch (SignatureException | IOException e) {
			e.printStackTrace();
			appendMessageToClientWindow(new Message(null,"AN ERROR OCCURED, EXITING..."));
		} finally{
			closeResources();
		}
	}// run end
	
	
	/**
	 * Unwraps the SealedObject and verifies the SignedObject.
	 * If this fails an error message is appended to the users screen.
	 * After that reacts to the type of Object being sent. 
	 * DisconnectMessage tells the client to disconnect.
	 * UserListMessage informs the client that there has been an update in 
	 * the userlist.
	 * Normal Messages are appended to the clients screen.
	 * @param sealed is the object that should be unwrapped, verified and
	 * handeld.
	 * @throws InvalidKeyException
	 * @throws ClassNotFoundException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws IOException
	 * @throws SignatureException
	 */
	private void handleSealedObject(SealedObject sealed) throws InvalidKeyException, 
	ClassNotFoundException, IllegalBlockSizeException, BadPaddingException, 
	IOException, SignatureException{
		
		SignedObject signed = reciever.convertSealedObject(sealed);
		if(reciever.validateSignedObject(signed)){
			Object object = reciever.convertSignedObject(signed);
			if(object instanceof DisconnectMessage){
				hasServer = false;
			}else if(object instanceof UserListMessage){
				gui.usersArea.setText(((UserListMessage)object).getUsernames());
			}else if(object instanceof Message){
				appendMessageToClientWindow((Message)object);
			}
		}else{
			appendMessageToClientWindow(new Message(null, "CLIENT RECIEVED A SIGNED OBJECT WITH AN INVALID SIGNATURE."));
		}
	}// handleSealedObject end
	
	
	/**
	 * Appends the content of a Message to the clients screen.
	 * @param m is the Message that schould be displayed.
	 */
	private void appendMessageToClientWindow(Message m){
		if(m.getUser() == null){ // null when from server
			gui.outputArea.append(m.getMessage() + "\n");
		}else{
			gui.outputArea.append(m.getUser() + ": " + m.getMessage() + "\n");
		}
	}// appendMessageToClientWindow end
	
	
	/**
	 * Gets the PublicKey sent from the server.
	 * If this is successful the Client sends its own PublicKey
	 * to the server.
	 * If any of these two operations fail the Client 
	 * closes its resources as no future communication can be encrypted.
	 * @param km is the KeyMessage that should be retrieved and saved.
	 */
	private void handleKeysFromServer(KeyMessage km){
		if(km.getKeytype() == KeyMessage.PUBLIC_KEY){
			if(!getServerPublicKey(km.getKey())){
				gui.outputArea.setText("FAILED TO GET SERVER KEY 1.\nCONNECTION ABORTED.");
				hasServer = false;
			}else{
				sendPublicKey();
			}
		}else if(km.getKeytype() == KeyMessage.SECRET_KEY){
			if(!recieveSymmetricKey(km)){
				gui.outputArea.setText("FAILED TO GET SERVER KEY 2.\nCONNECTION ABORTED.");
				hasServer = false;
			}else{
				initVerifiers();	 
			}
		}
	}// handleKeyFromServer end
	
	
	/**
	 * Takes the base64 coded String and decodes it to a byte array.
	 * Then takes the byte array and creates the servers PublicKey. 
	 * @param key is the servers PublicKey in a base64 encoded String.
	 * @return true if the operation is successful, else false.
	 */
	private boolean getServerPublicKey(String key){
		try {
			byte[] publicKeyBytes = Base64.getDecoder().decode(key);
			serverPublicKey = KeyFactory.getInstance(KEY_PAIR_ALGO).
					generatePublic(new X509EncodedKeySpec(publicKeyBytes));
			System.out.println("CLIENT SAVED SERVERS PUBLIC KEY");
			return true;
		} catch (InvalidKeySpecException | NoSuchAlgorithmException | IllegalArgumentException e) {
			return false;
		}
	}// getKey end
	
	
	/**
	 * Sends the Clients PublicKey after encoding it to a base64 String
	 * and wrapping it in a KeyMessage.
	 * @throws IOException
	 */
	private boolean sendPublicKey(){
		try{
			String keyText = Base64.getEncoder().
					encodeToString(clientPublicKey.getEncoded());
			outputStream.writeObject(new KeyMessage(user, keyText, KeyMessage.PUBLIC_KEY));
			outputStream.flush();	
			System.out.println("CLIENT SENDS ITS PUBLIC KEY");
			return true;
		} catch(IOException e){
			e.printStackTrace();
		}
		return false;
	}// sendPublicKey end
	
	
	/**
	 * Decodes the base64 String sent by the server, turning it into a byte array.
	 * The decoded byte array is then encrypted with the cipher that was initialized by the
	 * server using the clients PublicKey. Now the client initializes it with its PrivateKey
	 * making it possible to use the cipher to decrypt the byte array. Then creating
	 * the SecretKey from the decryptet byte array that will be used to encrypt and 
	 * decrypt SealedObjects in all future communication.
	 * @param km is the KeyMessage that contains the base64 decoded String.
	 * @return true if the operation is successful and false if it fails.
	 */
	private boolean recieveSymmetricKey(KeyMessage km){
		try {
            cipherKeyPair.init(Cipher.DECRYPT_MODE, clientPrivateKey);          
            byte[] ciphertextBytes = Base64.getDecoder().decode(km.getKey());
            byte[] decryptedBytes = cipherKeyPair.doFinal(ciphertextBytes);            
            secretKey = new SecretKeySpec(decryptedBytes, 0, decryptedBytes.length, SECRET_KEY_ALGO);
            System.out.println("CLIENT RECIEVED SYMMETRIC KEY FROM SERVER");
            return true;
		} catch (IllegalBlockSizeException | IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvalidKeyException | BadPaddingException e) {
			e.printStackTrace();
		} 
		return false;
	}// recieveSymmetricKey end
	
	
	 
	/**
	 * Sends a message to the server, telling it to disconnect since that is 
	 * what the user is doing. If keys have been exchanged a Signed and Sealed object is sent.
	 * Otherwise a simple DisconnectMessage is sent.
	 */
	void disconnectServer(){
		try {
			if(hasKeys){
				SignedObject signed = sender.createSignedObject(new DisconnectMessage());
				SealedObject sealed = sender.createSealedObject(signed);
				outputStream.writeObject(sealed);
				outputStream.flush();
			}else{
				outputStream.writeObject(new DisconnectMessage());
				outputStream.flush();
			}
		} catch (IOException | InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException | SignatureException e) {
			e.printStackTrace();
		} 
	}// disconnectServer end
	
	
	/**
	 * When the Client has sent its PublicKey and the generated symmetric/secret key successfully - 
	 * two object are created and used to decrypt and encrypt all future messages. 
	 * The class 'Verifier' exists as both the Client and Server needs to go through the same 
	 * steps when decrypting and encrypting messages. 
	 */
	private void initVerifiers(){
		sender = new Verifier(secretKey, cipherSecretKey, clientPublicKey, clientPrivateKey, signature);
		reciever = new Verifier(secretKey, cipherSecretKey, serverPublicKey, clientPrivateKey, signature); 
		hasKeys = true;
	}//initVerifiers end

	
	
	
}// Client end




