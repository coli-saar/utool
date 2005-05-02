import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;


/**
 * Server: baut das Lexikon, und gibt auf Anfragen des Clients
 * die entsprechenden Baeume zu einem Lexikoneintrag zurueck
 * muss mit der Option -Xmx256M aufgerufen werden, sonst 
 * OutOfMemory Exception
 */
public class Server {

private static String helpMessage = "Parameters of the Server:\n-p, --port <arg>: define port number (Required Parameter)\n-d, --debug     : Debug-Modus; display results (Optional Parameter)\n-h, --help      : display this\n";

  public static void main(String asArgs[]) {
    try {
	ConvenientGetopt cg = new ConvenientGetopt("Server", 
						   "java Server [options]",
						   "");
	 cg.addOption('d', "debug", ConvenientGetopt.NO_ARGUMENT,
        		"Run in debug mode", null);
	 cg.addOption('p', "port", ConvenientGetopt.REQUIRED_ARGUMENT,
			  "Use port <arg>", null);
	 cg.addOption('h', "help", ConvenientGetopt.NO_ARGUMENT,
		      "Display help", null);
	 cg.parse(asArgs);
        
        // extract arguments
	 
	 if (cg.hasOption('h')){
	     System.out.println(helpMessage);
	     return;}
	 
        boolean debugMode = cg.hasOption('d');
	int iPort = Integer.parseInt(cg.getValue('p'));

	// create a server socket
	ServerSocket sockServ = new ServerSocket(iPort);
	// build the lexikon
	System.out.println("[Server] building Lexicon");
	XDGMain main = new XDGMain();
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
	    System.out.println("[Server] sending Output \n");
	    if (debugMode){
		System.out.println("[Server] Output:\n"+outPut);}
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
      System.out.println(helpMessage);
    } catch (ArrayIndexOutOfBoundsException aioobe) {
      System.out.println(helpMessage);
    }
  }
}
