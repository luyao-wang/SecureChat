package message;
import java.io.Serializable;

/**
 * Used to send Keys between client and server. 
 * Static final ints are used to indicate what kind of key it is.
 * These public static ints should be used when creating the
 * KeyMessage.
 *
 */
public class KeyMessage implements Serializable{

	private static final long serialVersionUID = 494833997187472293L;
	public static final int SECRET_KEY = 1;	 
	public static final int PUBLIC_KEY = 2;
	private String user, key;
	private int keyType;
	private String invalidTypeMessage = "Invalid key type argument. Use static variables to define type.";
	 
	/**
	 * Constructs a KeyMessage.
	 * @param user is the username of the client. This is not used by the Server.
	 * @param key is a base64 representation of a keys byte array.
	 * Used to recreate the key that was sent.
	 * @param type determines what type of Key that is being sent.
	 */
	public KeyMessage(String user, String key, int type){
		this.user = user;
		this.key = key;
		if(!validKeytype(type)){
			throw new IllegalArgumentException(invalidTypeMessage);
		}
		this.keyType = type;
	}
	
	/**
	 * Used by the server to save the username of the client.
	 * Not used by Client as client can only recieve key from server.
	 * @return the users chosen username.
	 */
	public String getUser(){
		return user;
	}
	

	public String getKey(){
		return key;
	}
	
	public int getKeytype(){
		return keyType;
	}
	
	private boolean validKeytype(int type){
		if(type == SECRET_KEY || type == PUBLIC_KEY){
			return true;
		}
		return false;
	}
	
}
