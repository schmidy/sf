package com.schmidt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.schmidt.server.domain.Client;

/**
 * A multithreaded test tool.  When a client connects the
 * server requests a client name by sending the client the
 * text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received.  After a client submits a unique
 * name, the server acknowledges with "NAMEACCEPTED".  Then
 * all messages from that client will be logged.
 *
 */
public class Server {

    /**
     * The port that the server listens on.  Static for now, can change to CLI arg in future
     */
    private static final int PORT = 9001;
   

    /**
     * The set of all test clients.  Maintained
     * so that we can check that new clients are not registering name
     * already in use.
     */
    private static ArrayList<Client> clients = new ArrayList<Client>();
    
    /**
     * Log4j used to handle logging
     */
    final static Logger log = Logger.getLogger(Server.class.getName());
    

    /**
     * The application main method, which just listens on a port and
     * spawns handler threads.
     */
    public static void main(String[] args) throws Exception {
    	log.debug("The test server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     */
    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        /**
         * Constructs a handler thread
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a
         * client name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set
         */
        public void run() {
            try {

                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                	boolean addClient = false;
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    
                    if (name == null) {
                        return;
                    }
                    
                    synchronized (clients) {
                    	if (clients.isEmpty()) {
                			addClient = true;;
                    	} else {
	                    	for (Client client: clients) {
	                    		if (!client.getName().equals(name)) {
	                    			addClient = true;
	                    		}
	                    	}
                    	}
	                    if (addClient) {
	                    	log.debug("Registering client: " + name);
                			Client client = new Client(name);

                            // Add the socket's print writer to the set of all writers so
                            // this client can receive broadcast messages.
                			out.println("NAMEACCEPTED");
                			client.setWriter(out);
                			out.println("STARTTEST");
                			client.setWriter(out);
                			
                			clients.add(client);
	                    	break;
	                    }
                    }
                }

                // Now that a successful name has been chosen, add the
                // socket's print writer to the set of all writers so
                // this client can receive messages.
                
                // Accept messages from this client and log them.
                while (true) {
                    String input = in.readLine();
                    
                    if (input == null) {
                        return;
                    } else if (input.equalsIgnoreCase("exit")) {
                    	//writer.println("MESSAGE " + name + ": " + input);
                    	log.debug("Exiting for client: " + name);
                    	break;
                    }
                   
                    for (Client client : clients) {
                    	log.debug("Message Recieved from: " + name + " Msg: " + input);
                        client.getWriter().println("MESSAGE " + name + ": " + input);
                    }
                }
            } catch (IOException e) {
            	log.error(e);
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
            	// Ensure that that no one else will be updating the clients while removing a client
            	synchronized (clients) {
	                if (name != null) {
	                	if (clients.size() == 1) {
	                		clients.clear();
	                	} else {
	                		Iterator<Client> iter = clients.iterator();
	                		while(iter.hasNext()){
	                			if (iter.next().getName().equals(name)) {
	                				log.debug("Removeing client: " + name);
	                				iter.remove();
	                			}
	                		}
	                	}
	                	
	                    log.debug("There are " + clients.size() + " clients");
	                }
	                try {
	                    socket.close();
	                } catch (IOException e) {
	                }
            	}
            }
        }
    }
}
