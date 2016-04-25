package message;
import java.io.Serializable;

/**
 * Simple message, storing a formatted String of the connected users names.
 * Sent by Server to all clients when users leave or connect to server.
 */
public class UserListMessage implements Serializable{

	private static final long serialVersionUID = 6060637838791254344L;
	private String usernames;

	public UserListMessage(String usernames){
		this.usernames = usernames;
	}
	
	public String getUsernames(){
		return usernames;
	}
}
