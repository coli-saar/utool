
package de.saar.coli.chorus.oracle;

import java.io.*;
import java.net.*;

public class Server {
    private ServerSocket serversocket;
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private int counter;

    public Server(int port) {
        socket = null;
        counter = 1;

        System.err.println("Accepting connections ...");

        try {
            serversocket = new ServerSocket(port);
            socket = serversocket.accept();

            System.err.println("New connection accepted " +
                               socket.getInetAddress() +
                               ":" + socket.getPort());
            
            input = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
            output = new PrintWriter(socket.getOutputStream(), true);
            
            serversocket.close();
        }  catch( IOException e ) {
            System.err.println(e);
        }
        

    }


    public String read() {
        if( socket == null )
            return null;

        try {
            String message = input.readLine();
            //      System.err.println((counter++) + " Incoming: " + message);
            
            if( message == null ) {
                System.err.println("Connection closed by client");
                close();
                return null;
            } else {
                return message;
            }
        } catch( IOException e ) {
            System.err.println(e);
            return null;
        }
    }

    public void write(String str) {
        //        System.err.println("Outgoing: " + str);
        if( socket != null ) {
            output.println(str);
        }
    }

    public void write(Message msg) {
        write(msg.encode());
    }

    public void close() {
        try {
            socket.close();
            serversocket.close();
            socket = null;
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }
}


