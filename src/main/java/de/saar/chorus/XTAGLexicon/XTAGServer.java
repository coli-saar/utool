package de.saar.chorus.XTAGLexicon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Server: baut das Lexikon, und gibt auf Anfragen des Clients
 * die entsprechenden Baeume zu einem Lexikoneintrag zurueck
 * muss mit der Option -Xmx256M aufgerufen werden, sonst 
 * OutOfMemory Exception
 */
public class XTAGServer {
	
	public static void main(String asArgs[]) {
		try {
			// get port number from parameter
			int iPort = Integer.parseInt(asArgs[0]);
			
			// create a server socket
			ServerSocket sockServ = new ServerSocket(iPort);
			// build the lexikon
			System.out.println("[Server] building Lexicon");
			SocketMain main = new SocketMain();
			main.run();
			System.out.println("[Server] Lexicon ready");
			
			// wait for connection request
			System.out.println("[Server] waiting for connection on Port "+iPort);
			Socket sockTalk = sockServ.accept();
			System.out.println("[Server] have connection");
			
			// get a buffered Reader to read from socket
			InputStream is        = sockTalk.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br     = new BufferedReader(isr);
			// get a printwriter to write to socket
			OutputStream os       = sockTalk.getOutputStream();
			PrintWriter pw        = new PrintWriter(os);
			
			
			System.out.println("[Server] Waiting for Input");
			pw.println("!start!");
			pw.flush();
			//wait for the input
			String sIn = br.readLine();
			while (!sIn.equals("!finish!")){
				System.out.println("[Server] received Input \""+sIn+"\"");
				StringBuffer outPut = new StringBuffer();
				outPut = main.lookUp(outPut, sIn);
				System.out.println("[Server] sending Output \n"+outPut);
				pw.println(outPut);
				pw.println("!finish!");
				pw.flush();
				System.out.println("[Server] waiting for Input");
				sIn = br.readLine();
			}
			System.out.println("[Server] received Input \"finish!\"");
			// finished
			System.out.println("[Server] terminating");
			sockTalk.close();
			sockServ.close();
			
		} catch (IOException ioe) {
			System.out.println("Exception:"+ioe.getMessage());
		} catch (NumberFormatException nfe) {
			System.out.println("[Server] Need a port number as parameter");
		} catch (ArrayIndexOutOfBoundsException aioobe) {
			System.out.println("[Server] Need a port number as parameter");
		}
	}
}
