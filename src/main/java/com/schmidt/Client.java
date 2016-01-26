package com.schmidt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import com.schmidt.client.test.StorageTest;

/**
 * Simple test client for driving storage test.  Client can be 
 * executed with command line parameters or graphically.  A GUI is displayed
 * but not required for the test to proceed.  Events are displayed in the UI as
 * well as on the console.  
 *
 * The client follows a simple protocol for processing commands to/from the server.
 * When the server sends "SUBMITNAME" the client replies with the
 * desired client name.  The server will keep sending "SUBMITNAME"
 * requests as long as the client submits client names that are
 * already in use.  
 */
public class Client {

    private BufferedReader in;
    private PrintWriter out;
    private JFrame frame = new JFrame("Client");
    private JTextField textField = new JTextField(40);
    private JTextArea messageArea = new JTextArea(8, 40);
    
    private String serverAddress;
    private String clientName;
    
    private Thread heartbeatThread;
    private StorageTest storageTest;
    private Thread statusUpdate;
    
    static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
    private static CommandLineParser parser = new DefaultParser();
    private CommandLine cmd;
    private Options options = new Options();

    private static int testTime;
    
    final static Logger log = Logger.getLogger(Client.class.getName());
    
	// Set Defaults
    // Default ChunkSize 10 MB
    private int chunkSize = (int) ((Math.pow(1024, 2)) * 10);
    //Default File Size 20 MB
    private int fileSize = 10485760 * 20;
    // Default server port 
    private static int SERVERPORT = 9001;


    /**
     * Client Constructor
     * Process any CLI arguments passed in
     * Setup the UI and attach listener for handling input and output 
     * from the UI
     * 
     * @param args
     */
    public Client(String[] args) {
    	
    	processCLIArgs(args);

        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        frame.pack();

        // Add Listeners
        textField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield by sending
             * the contents of the text field to the server.    Then clear
             * the text area in preparation for the next message.
             */
            public void actionPerformed(ActionEvent e) {
            	String input = textField.getText();
                out.println(input);
                textField.setText("");
            }
        });
    }
    
    /**
     * Use Apache command line parser to process arguments
     * @param args
     */
    private void processCLIArgs (String[] args) {
    	
    	// Add all command line arguments to options
    	options.addOption("h", false, "Display Help");    	
    	options.addOption("t", true, "Length of time in seconds for test");
    	options.addOption("s", true, "Server FQDN or IP address");
    	options.addOption("n", true, "Client name.");
    	options.addOption("c", true, "Data chunk size in bytes.");
    	options.addOption("f", true, "File size in bytes.");
    	
    	try {
			cmd = parser.parse( options, args);
			
			if (cmd.hasOption("h")) {
				help();
			}
			
			// Look for all required options first and exit if not found.
			if (!cmd.hasOption("t")) {
				System.out.println("Missing required option: t");
				help();
			}

			if (cmd.hasOption("t")) {
				testTime = Integer.valueOf(cmd.getOptionValue("t"));
				log.debug("Setting test time: " + testTime + " seconds.");
			}
			
			if (cmd.hasOption("s")) {
				serverAddress = cmd.getOptionValue("s");
				log.debug("Setting server address to: " + serverAddress);
			}

			if (cmd.hasOption("n")) {
				clientName = cmd.getOptionValue("n");
				log.debug("Setting client name to: " + clientName);
			}
			
			if (cmd.hasOption("c")) {
				chunkSize = Integer.valueOf(cmd.getOptionValue("c"));
				log.debug("Setting chunk size to: " + chunkSize + " bytes");
			}
			
			if (cmd.hasOption("f")) {
				fileSize = Integer.valueOf(cmd.getOptionValue("f"));
				log.debug("Setting file size to: " + fileSize + " bytes");
			}

			
		} catch (ParseException e1) {
			e1.printStackTrace();
		} catch (NumberFormatException ne) {
			log.error("Test time must be number");
			help();
		}

    }
    
    /**
     * Print help to CLI
     */
	private void help() {
		// This prints out some help
		HelpFormatter formater = new HelpFormatter();

		formater.printHelp("Main", options);
		System.exit(0);
	}


    /**
     * UI: Prompt for and return the address of the server.
     */
    private String getServerAddressUI() {
        
    	return (String) JOptionPane.showInputDialog(
            frame,
            "Enter IP Address ro FQDN of the Server:",
            "Storage Test Client",
            JOptionPane.QUESTION_MESSAGE, null,null,"localhost");
    }

    /**
     * UI: Get client name.  If not passed in on command line, 
     * prompt for and return the desired client name.
     */
	private String getName() {

		return (clientName != null) ? clientName : JOptionPane.showInputDialog(
				frame, "Choose a client name:", "Client name selection",
				JOptionPane.PLAIN_MESSAGE);
	}

	/**
	 * Connects to the server then enters the processing loop.
	 * @param start
	 * @throws IOException
	 */
	private void run(long startTime) throws IOException {
		
		log.debug("Start Time: " + startTime);

		// Make connection and initialize streams
		if (serverAddress == null || serverAddress.isEmpty()) {
			serverAddress = getServerAddressUI();
		}

		Socket socket = new Socket(serverAddress, SERVERPORT);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);

		// Process all messages from server, according to the protocol.
		while (true) {
			String line = "";
			try {
				line = in.readLine();
			} catch (SocketException se) {
				exit();
				Thread.currentThread().interrupt();
			}

			if (line == null) {
				break;
			} else if (line.startsWith("SUBMITNAME")) {

				clientName = getName();
				out.println(clientName);
				log.debug("SUBMITNAME requested");

			} else if (line.startsWith("NAMEACCEPTED")) {

				textField.setEditable(true);
				log.debug("NAMEACCEPTED received");

			} else if (line.startsWith("MESSAGE")) {

				messageArea.append(line.substring(8) + "\n");
				log.debug("MESSAGE received: " + line);

			} else if (line.startsWith("STARTTEST")) {

				log.debug("Starting Heartbeat: "
						+ dateFormat.format(new Date()));
				heartbeatThread = new Thread(new Heartbeat(out));
				heartbeatThread.start();

				log.debug("Starting Storage Performance Test: "
						+ dateFormat.format(new Date()));
				storageTest = new StorageTest(clientName, fileSize, chunkSize, testTime, out);
				storageTest.start();

				statusUpdate = new Thread(new StatusUpdate(out));
				statusUpdate.start();
				
			}
			
			// Execute until test time has been reached
			// Very simple way to track test execute test time
			if (System.currentTimeMillis()-startTime > testTime*1000) {
				long endTime = System.currentTimeMillis();
				log.debug("End Time: " + endTime);
				log.debug("Test time elapsed: " + ((endTime - startTime) / 1000) + " secs...");
				out.println("exit");
			}
		}

		try {
			socket.close();
		} catch (IOException e) {
		}

		return;
	}
    
    
    /** Perform any steps to clean up and exit the client
     *  Ensure that threads are exited
     */
    private void exit () {
    	heartbeatThread.interrupt();
    	storageTest.interrupt();
    	statusUpdate.interrupt();
    }
    
    /**
     * Send Heartbeat to server every 5 seconds
     */
    private static class Heartbeat extends Thread {
    	
    	private PrintWriter printWriter;
    	
    	public Heartbeat (PrintWriter printWriter) {
    		this.printWriter = printWriter;
    	}
    	
    	public void run () {
    		while (true) {
    			printWriter.println("HeartBeat: " +  dateFormat.format(new Date()));
    			try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					break;
				}
    		}
    	}    	
    }
    
    /**
     *     Send CPU and Memory information to server every 10 seconds
     */
    private static class StatusUpdate extends Thread {
    	
    	private PrintWriter printWriter;
    	
    	public StatusUpdate (PrintWriter printWriter) {
    		this.printWriter = printWriter;
    	}
    	
    	public void run () {
    		while (true) {
    			printWriter.println("Status Memory: " +  Runtime.getRuntime().totalMemory());
    			try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					break;
				}
    		}
    	}    	
    }


    /**
     * Runs the client as an application with a closeable frame.
     */
    public static void main(String[] args) throws Exception {
    	
        Client client = new Client(args);
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        
        client.run(System.currentTimeMillis());
        client.exit();
        
        client.frame.dispose();
    }
}
