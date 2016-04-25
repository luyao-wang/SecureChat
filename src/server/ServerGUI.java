package server;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
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
 * GUI for the server. 
 * Starts a server instance when the user has entered a valid port.
 * 
 * @author Tomas
 */
class ServerGUI extends JFrame{
	
	private static final long serialVersionUID = -262454029120918151L;
	private static final String DEFAULT_PORT = "2000";
	private static final int START_FRAME_WIDTH = 260;
	private static final int START_FRAME_HEIGHT = 230;
	private static final int ACTIVE_FRAME_WIDTH = 450;
	private static final int ACTIVE_FRAME_HEIGHT = 400;
	private Color backgroundColor = new Color(93, 93, 93);
	private Color errorColor = new Color(218, 181, 39);
	private Color btnHover = new Color(59,57,58);
	private Color btnBorder = new Color(79,77,78);
	private Color btnBack = new Color(45,45,45);
	private Server server;
	private int port;
	
	private Container contentPane;
	private SpringLayout layout;
	private JScrollPane scrollContent, scrollUsers;
	private JTextField portField;	
	private JTextArea messageToUser;
	String startTitle, activeTitle;
	JTextArea outputArea, usersArea;
	
	
	/**
	 * Constructs the ServerGUI, calling helper methods setIcons and buildServerStart.
	 */
	ServerGUI(){
		try {
			startTitle = "SERVER ON: " + InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			startTitle = "SERVER ON: UNKNOWN";
		}
		setResizable(false);
		contentPane = getContentPane();
		contentPane.setPreferredSize(new Dimension(START_FRAME_WIDTH, START_FRAME_HEIGHT));
        layout = new SpringLayout();
        contentPane.setLayout(layout); 
        contentPane.setBackground(backgroundColor);
        setIcons();
        buildServerStart();
        
	    setLocationRelativeTo(null);
	    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	    addWindowListener(new WindowAdapter(){
	        public void windowClosing(WindowEvent event){
	        	if(server != null){
		    		server.close(port);
		    	}
		    	setVisible(false);
				dispose();
	        }
	    });
	    setVisible(true);
	}// constructor end
	
	
	/**
	 * Builds the startscreen and adds ActionListners and KeyListners
	 * handling user events when buttons are clicked.
	 */
	private void buildServerStart(){
		contentPane.removeAll();
		JLabel heading = null;
		try {
    		ImageIcon icon = new ImageIcon(this.getClass().getResource("/icons/whisper_server_heading.png")); 
    		heading = new JLabel(icon);
    		contentPane.add(heading);
    		layout.putConstraint(SpringLayout.WEST, heading, 5, SpringLayout.WEST, contentPane);
    	    layout.putConstraint(SpringLayout.NORTH, heading, 5, SpringLayout.NORTH, contentPane);
    	} catch (Exception e) {
			e.printStackTrace();
		}		 
        
        JLabel portLabel = new JLabel("PORT");
        portLabel.setFont(new Font("SanSerif", Font.BOLD, 13));
		contentPane.add(portLabel);
		contentPane.add(portField = new JTextField(DEFAULT_PORT, 4));
        layout.putConstraint(SpringLayout.WEST, portLabel, (START_FRAME_WIDTH/2)-25, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, portLabel, 81, SpringLayout.NORTH, heading);
        layout.putConstraint(SpringLayout.WEST, portField, (START_FRAME_WIDTH/2)-25, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, portField, 20, SpringLayout.NORTH, portLabel);  
	    
		JButton startBtn = new JButton("START");
		startBtn.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	controlAndStart(portField.getText());
		    }
		});
		startBtn.addKeyListener(new KeyListener(){
	    	@Override
	    	public void keyPressed(KeyEvent eve) {
		    	if(eve.getKeyCode() == KeyEvent.VK_ENTER){
		    		controlAndStart(portField.getText());
		    	}
	    	}
	    	public void keyReleased(KeyEvent e) {}
	    	public void keyTyped(KeyEvent e) {}
	    });
		contentPane.add(startBtn);
		layout.putConstraint(SpringLayout.WEST, startBtn, 157, SpringLayout.WEST, contentPane);
		layout.putConstraint(SpringLayout.NORTH, startBtn, 45, SpringLayout.NORTH, portField);
		createSimpleButton(startBtn);
		
		JButton exitBtn = new JButton("EXIT");		
		exitBtn.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
				System.exit(0);
		    }
		});
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
		contentPane.add(exitBtn);
		layout.putConstraint(SpringLayout.WEST, exitBtn, 30, SpringLayout.WEST, contentPane);
		layout.putConstraint(SpringLayout.NORTH, exitBtn, 45, SpringLayout.NORTH, portField);
		createSimpleButton(exitBtn);
		
		
		contentPane.add(messageToUser = new JTextArea());
		messageToUser.setBackground(contentPane.getBackground());
		messageToUser.setForeground(errorColor);
		messageToUser.setEditable(false);
		messageToUser.setLineWrap(true);
		messageToUser.setWrapStyleWord(true);
		messageToUser.setPreferredSize(new Dimension(160,35));
        layout.putConstraint(SpringLayout.WEST, messageToUser, (START_FRAME_WIDTH/2)-80, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, messageToUser, 35, SpringLayout.NORTH, exitBtn);
		
	    pack();
	    revalidate();
	    repaint();
	}// buildServerStart end
	
	
	/**
	 * Builds the look of the active server.
	 */
	private void buildActiveServer(){
		contentPane.removeAll();
		contentPane.setPreferredSize(new Dimension(ACTIVE_FRAME_WIDTH, ACTIVE_FRAME_HEIGHT));
		activeTitle = startTitle + " - PORT: " + port + " - N CLIENTS: ";
		setTitle(activeTitle + "0");
		
		contentPane.add(scrollContent = new JScrollPane(outputArea = new JTextArea()));
		outputArea.setEditable(false);
	    layout.putConstraint(SpringLayout.WEST, scrollContent, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, scrollContent, 5, SpringLayout.NORTH, contentPane);
        scrollContent.setPreferredSize(new Dimension(315,391));
        outputArea.setLineWrap(true);
	    scrollContent.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	 
	    contentPane.add(scrollUsers = new JScrollPane(usersArea = new JTextArea()));
	    usersArea.setEditable(false);
	    layout.putConstraint(SpringLayout.WEST, scrollUsers, 5, SpringLayout.EAST, scrollContent);
        layout.putConstraint(SpringLayout.NORTH, scrollUsers, 5, SpringLayout.NORTH, contentPane);
        scrollUsers.setPreferredSize(new Dimension(120,391));
        usersArea.setLineWrap(true);
	    scrollContent.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	    outputArea.append("SERVER STARTED " + Server.dateFormat.format(new Date()) + "\n");
	    
	    pack();
	    revalidate();
	    repaint();
	}// buildActiveServer end
	
	
	/**
	 * Checks that the user has entered something in the port field and sends
	 * this information to the startServer method. 
	 * @param portString
	 */
	private void controlAndStart(String portString){
		if(portString.isEmpty()){
    		messageToUser.setText("You have to enter a port.");
    	}else{
    		startServer(portString);
    	}
	}// controlAndStart end
	
	
	/**
	 * Validates the user input and tries to start a server at the supplied port.
	 * The server gets a refference to the ServerGUI making it possible to append 
	 * input to the server area.
	 * @param portString
	 */
	private void startServer(String portString){
		try{
			port = Integer.parseInt(portString);
			if(port < 1 || port > 65535){
				throw new IllegalArgumentException("Enter a port between 1 and 65535.");
			}
			server = new Server(new ServerSocket(port), this);
			buildActiveServer();
			new Thread(server).start();
		} catch(NumberFormatException ex){
			messageToUser.setText("Port has to be a number.");
		} catch(IllegalArgumentException ex){
			messageToUser.setText(ex.getMessage());
		} catch (UnknownHostException ex){
			messageToUser.setText("Failed to retrieve hostname from computer.");
		} catch (IOException ex) {
			messageToUser.setText("Failed to open port.");
		} catch (NoSuchAlgorithmException e) {
			messageToUser.setText("Error, can't start application.");
		} catch (NoSuchPaddingException e) {
			messageToUser.setText("Error, can't start application.");
		} 
	}// startServer end
	
	
	/**
	 * Loads icons from the icons folder. The dimensions are the most common
	 * icon sizes in the most popular operating systems and are the sizes that 
	 * the icons have been saved in.
	 */
	private void setIcons(){
		int [] dimensions = {16,18,22,24,32,48,72,96,128,180,256,512,1024};
		List<BufferedImage> icons = new ArrayList<BufferedImage>();
        for(int i : dimensions){
        	try {
        		URL url = this.getClass().getResource("/icons/server_icon_"+i+"x"+i+".png");
        		BufferedImage img = ImageIO.read(url);
        		icons.add(img);
        	} catch (IOException | NullPointerException e) {
        		break;
        	}
        }
        setIconImages(icons);
	}// setIcons end
	
	
	/**
	 * Gives a JButton a simple style.
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
		    	btn.setBackground(btnHover);
		    }

		    public void mouseExited(MouseEvent evt) {
		    	btn.setBackground(btnBack);
		    }
		});
	}// createSimpleButton end
	
	
	
}// ServerGUI end



