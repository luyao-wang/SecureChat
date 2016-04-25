package client;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.NoSuchPaddingException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * A simple GUI for the chat client.
 * Takes user input, validates it and starts chat client if 
 * everything is valid and server exists.
 * 
 * @author Tomas
 */
class ClientGUI extends JFrame{
	private static final long serialVersionUID = -8509590566551245416L;
	private static final String DEFAULT_PORT = "2000";
	private static final String DEFAULT_HOST = "127.0.0.1";
	private static final int START_FRAME_WIDTH = 240;
	private static final int START_FRAME_HEIGHT = 275;
	private static final int ACTIVE_FRAME_WIDTH = 350;
	private static final int ACTIVE_FRAME_HEIGHT = 400;
	private Color backgroundColor = new Color(93, 93, 93);
	private Color errorColor = new Color(218, 181, 39);
	private Color btnHover = new Color(59,57,58);
	private Color btnBorder = new Color(79,77,78);
	private Color btnBack = new Color(45,45,45);
	
	private boolean disabled;
	private String host;
	private int port;
	//CONNECT
	private Container contentPane;
	private SpringLayout layout; 
	private JTextField userFieldLogin, hostField, portField;
	private JTextArea messageToUser;
	//CHAT
	JTextField inputField;
	JButton sendBtn;
	private Client client;
	JTextArea outputArea, usersArea;
	
	
	/**
	 * Constructs the start window of the chat client. 
	 * Calls helper methods setIcons and buildStart.
	 * Adds a WindowListner listening to window closing event. 
	 * Closes the server connection if the client is connected to a server.
	 * If not, just closes window.
	 * 
	 */
	public ClientGUI(){
	    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	    setResizable(false);
	    this.disabled = false;
	    layout = new SpringLayout();
		contentPane = getContentPane();
        contentPane.setLayout(layout); 
        contentPane.setPreferredSize(new Dimension(START_FRAME_WIDTH, START_FRAME_HEIGHT));
        contentPane.setBackground(backgroundColor);

        setIcons();
        buildStart();
		setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
	        public void windowClosing(WindowEvent e) {
	        	if(client != null){
	        		if(client.socket != null){		//If we're in a chat,
	        			client.disconnectServer();  //inform server of closing
	        		}
	        		client.closeResources();    	//close resources, interrupting readObject, catch exception
	        	}
	        	setVisible(false);
	        	dispose();
	        }
	    });
        setVisible(true);
	}// Constructor end
	
	
	/**
	 * Build the initial look of the Client GUI.
	 * Adding Action listners to buttons that call helper methods that validate
	 * the user input before trying to start the client.
	 */
	private void buildStart(){
		setTitle("Whisper");
		JLabel heading = null;
		try {
    		ImageIcon icon = new ImageIcon(this.getClass().getResource("/icons/whisper_heading.png")); 
    		heading = new JLabel(icon);
    		contentPane.add(heading);
    		layout.putConstraint(SpringLayout.WEST, heading, 5, SpringLayout.WEST, contentPane);
    	    layout.putConstraint(SpringLayout.NORTH, heading, 5, SpringLayout.NORTH, contentPane);
    	} catch (Exception e) {
			e.printStackTrace();
		}		
    	        
        JLabel hostLabel = new JLabel("HOST");
		contentPane.add(hostLabel);
        contentPane.add(hostField = new JTextField(DEFAULT_HOST, 9));
        layout.putConstraint(SpringLayout.WEST, hostLabel, (START_FRAME_WIDTH/2)-100, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, hostLabel, 85, SpringLayout.NORTH, heading);
        layout.putConstraint(SpringLayout.WEST, hostField, (START_FRAME_WIDTH/2)-100, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, hostField, 17, SpringLayout.NORTH, hostLabel);   
        
        JLabel portLabel = new JLabel("PORT");
		contentPane.add(portLabel);
        contentPane.add(portField = new JTextField(DEFAULT_PORT, 4));
        layout.putConstraint(SpringLayout.WEST, portLabel, (START_FRAME_WIDTH/2)-80, SpringLayout.EAST, hostField);
        layout.putConstraint(SpringLayout.NORTH, portLabel, 85, SpringLayout.NORTH, heading);
        layout.putConstraint(SpringLayout.WEST, portField, (START_FRAME_WIDTH/2)-80, SpringLayout.EAST, hostField);
        layout.putConstraint(SpringLayout.NORTH, portField, 17, SpringLayout.NORTH, portLabel);   
        
     
        JLabel userLabelLogin = new JLabel("USERNAME");
		contentPane.add(userLabelLogin);
        contentPane.add(userFieldLogin = new JTextField("", 11));
        layout.putConstraint(SpringLayout.WEST, userLabelLogin, (START_FRAME_WIDTH/2)-100, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, userLabelLogin, 35, SpringLayout.NORTH, hostField);
        layout.putConstraint(SpringLayout.WEST, userFieldLogin, (START_FRAME_WIDTH/2)-100, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, userFieldLogin, 17, SpringLayout.NORTH, userLabelLogin);            
		
        JButton connectBtn = new JButton("CONNECT");
		connectBtn.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	validateAndConnect();
		    }
		});
		contentPane.add(connectBtn);
		layout.putConstraint(SpringLayout.WEST, connectBtn, (START_FRAME_WIDTH/2), SpringLayout.WEST, contentPane);
		layout.putConstraint(SpringLayout.NORTH, connectBtn, 35, SpringLayout.NORTH, userFieldLogin);
		connectBtn.addKeyListener(new KeyListener(){
	    	@Override
	    	public void keyPressed(KeyEvent eve) {
		    	if(eve.getKeyCode() == KeyEvent.VK_ENTER){
		    		validateAndConnect();
		    	}
	    	}
	    	public void keyReleased(KeyEvent e) {}
	    	public void keyTyped(KeyEvent e) {}
	    });
		createSimpleButton(connectBtn);
		
        JButton exitBtn = new JButton("EXIT");		
		exitBtn.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	System.exit(0);
		    }
		});
		contentPane.add(exitBtn);
		layout.putConstraint(SpringLayout.WEST, exitBtn, (START_FRAME_WIDTH/2)-100, SpringLayout.WEST, contentPane);
		layout.putConstraint(SpringLayout.NORTH, exitBtn, 35, SpringLayout.NORTH, userFieldLogin);
		exitBtn.addKeyListener(new KeyListener(){
	    	@Override
	    	public void keyPressed(KeyEvent eve) {
		    	if(eve.getKeyCode() == KeyEvent.VK_ENTER){
		    		System.exit(0);
		    	}
	    	}
	    	public void keyReleased(KeyEvent e) {}
	    	public void keyTyped(KeyEvent e) {}
	    });
		createSimpleButton(exitBtn);
		
		contentPane.add(messageToUser = new JTextArea());
		messageToUser.setBackground(contentPane.getBackground());
		messageToUser.setEditable(false);
		messageToUser.setLineWrap(true);
		messageToUser.setWrapStyleWord(true);
		messageToUser.setPreferredSize(new Dimension(190,40));
		messageToUser.setForeground(errorColor);
        layout.putConstraint(SpringLayout.WEST, messageToUser, (START_FRAME_WIDTH/2)-100, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, messageToUser, 35, SpringLayout.NORTH, exitBtn);
        
        pack();
	}// buildLogin end
	
	
	/**
	 * Build the active chat client.
	 * Information can be sent both by pressing ENTER or clicking the 'SEND' button. 
	 */
	private void buildChat(){
		contentPane.removeAll();
		contentPane.setPreferredSize(new Dimension(ACTIVE_FRAME_WIDTH, ACTIVE_FRAME_HEIGHT));
		setTitle("Server: " + host + " Port: " + port);
		JScrollPane scrollContent = new JScrollPane(outputArea = new JTextArea());
		contentPane.add(scrollContent);
	    outputArea.setEditable (false);
	    layout.putConstraint(SpringLayout.WEST, scrollContent, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, scrollContent, 5, SpringLayout.NORTH, contentPane);
        scrollContent.setPreferredSize(new Dimension(235,366));
        outputArea.setLineWrap(true);
	    scrollContent.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
	    JScrollPane scrollUsers = new JScrollPane(usersArea = new JTextArea());
	    contentPane.add(scrollUsers);
	    scrollUsers.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	    usersArea.setLineWrap(true);
	    usersArea.setEditable (false);
	    layout.putConstraint(SpringLayout.WEST, scrollUsers, 5, SpringLayout.EAST, scrollContent);
        layout.putConstraint(SpringLayout.NORTH, scrollUsers, 5, SpringLayout.NORTH, contentPane);
        scrollUsers.setPreferredSize(new Dimension(101,366));
        
	    contentPane.add(inputField = new JTextField("",21));
	    layout.putConstraint(SpringLayout.WEST, inputField, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.SOUTH, inputField, 25, SpringLayout.SOUTH, scrollContent);
	    inputField.requestFocus();
	    
	    sendBtn = new JButton("SEND");
	    contentPane.add(sendBtn);
	    sendBtn.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	if(client.hasServer){
		    		sendUserInput();
		    	}
		    }
		});
	    layout.putConstraint(SpringLayout.WEST, sendBtn, 4, SpringLayout.EAST, inputField);
        layout.putConstraint(SpringLayout.SOUTH, sendBtn, 24, SpringLayout.SOUTH, scrollContent);
        sendBtn.setPreferredSize(new Dimension(101,18));
        createSimpleButton(sendBtn);
        
        inputField.addKeyListener(new KeyListener(){
	    	@Override
	    	public void keyPressed(KeyEvent eve) {
		    	if(eve.getKeyCode() == KeyEvent.VK_ENTER){
		    		if(client.hasServer){
			    		sendUserInput();
			    	}
		    	}
	    	}
	    	public void keyReleased(KeyEvent e) {}
	    	public void keyTyped(KeyEvent e) {}
	    });
	    
        revalidate();
	    repaint();
	    pack();
	}// buildChat end
	
	
	/**
	 * If there is content in the input field, send it.
	 * Called when the user presses ENTER or clicks the 
	 * SEND button.
	 */
	private void sendUserInput(){
		String text = inputField.getText();
		if(!text.isEmpty()){
			inputField.setText("");
			client.sendMessage(text);
		}
	}// sendUserInput end
	
	
	/**
	 * If all fields have been filled try to connect user.
	 * If connection is successful load chat gui and start client.
	 */
	private void validateAndConnect(){
		String host = hostField.getText();
    	String portString = portField.getText();
    	String user = userFieldLogin.getText();
    	if(host.isEmpty() || portString.isEmpty() || user.isEmpty()){
    		messageToUser.setText("All fields have to be filled.");
    	}else if(connect(host, portString, user)){
			buildChat();
			client.start();
    	}
	}// validateAndConnect end
	
	
	/**
	 * Take the user input and try to create a new Client object.
	 * @param enteredHost is the host entered by the user.
	 * @param enteredPort is the port entered by the user.
	 * @param user is the username that the user has supplied.
	 * @return true if all input is valid and a Client could be created, 
	 * otherwise return false.
	 */
	private boolean connect(String enteredHost, String enteredPort, String user){
		if(validIP(enteredHost)){
			try{
				int nameLength = user.trim().length();
				if(nameLength < 3 || nameLength > 10){
					throw new IllegalArgumentException("Username has to be 3-10 charachters long.");
				}
				port = Integer.parseInt(enteredPort);
				if(port < 0 || port > 65535){
					throw new IllegalArgumentException("Port has to be a number between 0 and 65535.");
				}
				host = enteredHost;
				client = new Client(user, this, new Socket(host, port));
				return true;
			} catch(NumberFormatException e) {
				messageToUser.setText("Port has to be a number.");
			} catch (UnknownHostException e) {
				messageToUser.setText("Couldn't find a valid host with the given IP address.");
			} catch(ConnectException e){
				messageToUser.setText("Couldn't connect to host: " + host + " at port " + port + ".");
			} catch (IOException e) {
				messageToUser.setText("Failed I/O operation. Are you sure that the server settings are correct?");
			} catch (IllegalArgumentException e) {
				messageToUser.setText(e.getMessage());
			} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
				messageToUser.setText("ERROR - Can't start program.");
				e.printStackTrace();
			}
		}// if end
		return false;
	}// connect end
	
	
	/**
	 * Determines if the host/IP address entered by the user is a valid hostname.
	 * @param address the IP address to analyze.
	 * @return true if the address is valid, otherwise false.
	 * @throws NumberFormatException if any of the four parts of the IP isn't 
	 * a number.
	 */
	private boolean validIP(String address) throws NumberFormatException{
		String [] addressString = address.split("[.]");
		if(addressString.length != 4){
			messageToUser.setText("Invalid IP address.");
			return false;
		}else{
			for(int i = 0; i < 4; ++i){
				try{
					int ipPart = Integer.parseInt(addressString[i]);
					if(ipPart < 0 || ipPart > 255){
						messageToUser.setText("Every part of the IP address has to be between 0 and 255.");
						return false;
					}
				} catch(NumberFormatException e){
					messageToUser.setText("Invalid IP address.");
					return false;
				}
			}// for end
		return true;
		}
	}// validIP end
	
	
	/**
	 * Load all the icons that are available and append them to the frame.
	 */
	private void setIcons(){
		//Most common icon sizes used by popular operating systems
		int [] dimensions = {16,18,22,24,32,48,72,96,128,180,256,512,1024}; 
		List<BufferedImage> icons = new ArrayList<BufferedImage>();
        for(int i : dimensions){
        	try {
        		URL url = this.getClass().getResource("/icons/client_icon_"+i+"x"+i+".png");
        		BufferedImage img = ImageIO.read(url);
        		icons.add(img);
        	} catch (IOException | NullPointerException e) {
        		break;
        	}
        }
        setIconImages(icons);
	}// setIcons end
	
	
	/**
	 * Remove the possibility for the user to enter input.
	 * This is done when the user is disconnected and the GUI isn't closed.
	 * This for example happens when the server is closed while the client
	 * is connected. 
	 */
	void disableUserInterface(){
		disabled = true;
		inputField.setEnabled(false);
		sendBtn.setEnabled(false);
		usersArea.setText("");
		requestFocus();
	}// disableUserInterface end
	
	
	
	/**
	 * Gives a Jbutton a simple style.
	 * @param btn is the button to style.
	 */
	private void createSimpleButton(JButton btn){
		btn.setContentAreaFilled(false);
		btn.setForeground(Color.WHITE);
		btn.setBackground(btnBack);
		btn.setOpaque(true);
		Border line = new LineBorder(btnBorder);
		Border margin = new EmptyBorder(5, 15, 5, 15);
		Border compound = new CompoundBorder(line, margin);
		btn.setBorder(compound);
		btn.setFocusPainted(false);
		btn.addMouseListener(new java.awt.event.MouseAdapter() {
		    public void mouseEntered(MouseEvent evt) {
		    	if(!disabled){
		    		btn.setBackground(btnHover);
		    	}
		    }

		    public void mouseExited(MouseEvent evt) {
		    	btn.setBackground(btnBack);
		    }
		});
		
	}// createSimpleButton end
	

}// ClientGUI end



