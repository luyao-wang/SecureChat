package message;
import java.io.Serializable;

/**
 * Stores a String that the user or server is sending.
 * Also stores the name of the sender.
 * When the server sends messages 'user' is set to null and the name isn't
 * displayed by server or client. 
 */
public class Message implements Serializable{
		
	private static final long serialVersionUID = 6938346704704015157L;
	private String message;
	private String user;
	
	public Message(String user, String message){
		this.user = user;
		this.message = message;
	}
	
	public String getMessage(){
		return message;
	}//getMessage end
	
	public String getUser(){
		return user;
	}//getUser end

	
}
