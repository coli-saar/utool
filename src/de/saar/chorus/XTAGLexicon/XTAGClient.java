package de.saar.chorus.XTAGLexicon;

import java.net.Socket;
import java.net.UnknownHostException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * Client
 * nimmt Anfragen zu Lexikoneintraegen entgegen und
 * gibt die Baeume aus, die vom Server kommen
 */

public class XTAGClient {

  public static void main(String asArgs[]) {
    try {
      // get Hostname and port number from params
      String sHost = asArgs[0];
      int iPort = Integer.parseInt(asArgs[1]);

      // create socket connecting to host at port
      System.out.println("[Client] Connecting to \""
			 +sHost+"\" on Port "+iPort);
      Socket sockTalk = new Socket(sHost, iPort);

      // get a buffered Reader to read from socket
      InputStream is        = sockTalk.getInputStream();
      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br     = new BufferedReader(isr);
      // get a printwriter to write to socket
      OutputStream os       = sockTalk.getOutputStream();
      PrintWriter pw        = new PrintWriter(os);

      // send message
      System.out.println("[Client] Waiting for Server");
      String sIn = br.readLine();
      if (!sIn.equals("!start!")){
	  System.out.println("[Client]Error: terminating");
	  sockTalk.close();
      }
      else{
	  System.out.println("[Client] Lexicon ready");
	  System.out.println("[Client] Waiting for Input. Type \"finish!\" to terminate");
	  BufferedReader stdIn = 
	      new BufferedReader(new InputStreamReader(System.in));
	  String userIn = stdIn.readLine();
	  while (!userIn.equals("finish!")){
	      System.out.println("[Client] Sending message \""
				 +userIn+"\"");
	      pw.println(userIn);
	      pw.flush();
	      System.out.println("[Client] received Input: ");
	      sIn = br.readLine();
	      while (!sIn.equals("!finish!")){
		  System.out.print(sIn+"\n");
		  sIn = br.readLine();
	      }
	      System.out.println("[Client] Waiting for Input. Type \"finish!\" to terminate");
	      userIn = stdIn.readLine();
	  }
	  System.out.println("[Client] Sending message \""
			     +userIn+"\"");
	  pw.println("!finish!");
	  pw.flush();
	  // terminating
	  System.out.println("[Client] terminating");
	  sockTalk.close();
      }
    }
    catch (UnknownHostException uhee) {
	System.out.println("[Client] Unknown host : "+asArgs[0]);
    } catch (IOException ioe) {
	System.out.println("[Client] Some IO Exception occurred");
    } catch (NumberFormatException nfe) {
	System.out.println("[Client] Need a port number as parameter");
    } catch (ArrayIndexOutOfBoundsException aioobe) {
	System.out.println("[Client] Need a host name,a port number and a message as parameters");
    }
  }
}
