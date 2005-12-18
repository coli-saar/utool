
package de.saar.chorus.ubench;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket serversocket;
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    
    private InetAddress clientAddress;
    private int clientPort;

    private int counter;

    public Server(int port) {
        socket = null;
        counter = 1;

        System.err.println("Accepting connections on port " + port + "...");

        try {
            serversocket = new ServerSocket(port);
            socket = serversocket.accept();

            clientAddress = socket.getInetAddress();
            clientPort = socket.getPort();
            
            System.err.println("New connection accepted " + clientAddress + 
                               ":" + clientPort);
            
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
                System.err.println("Connection closed by client.");
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
    /**
     * @return Returns the clientAddress.
     */
    public String getClientAddress() {
        return clientAddress.getHostName();
    }
    /**
     * @return Returns the clientPort.
     */
    public int getClientPort() {
        return clientPort;
    }
}


