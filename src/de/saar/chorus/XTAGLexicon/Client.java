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

public class Client {

private static String helpMessage = "Parameters of the Client:\n-p, --port <arg>    : define port number (Required Parameter)\n-m, --machine <arg> : define the Host name; default value localhost (Optional Parameter)\n-h, --help          : display this\n";


  public static void main(String asArgs[]) {
    try {
	ConvenientGetopt cg = new ConvenientGetopt("Client", 
						   "java Client [options]",
						   "");
	 cg.addOption('m', "machine", ConvenientGetopt.REQUIRED_ARGUMENT,
		      "Use host <arg>", null);
	 cg.addOption('p', "port", ConvenientGetopt.REQUIRED_ARGUMENT,
		      "Use port <arg>", null);
	 cg.addOption('x', "xml", ConvenientGetopt.NO_ARGUMENT,
		      "Print only xml code", null);
	 cg.addOption('h', "help", ConvenientGetopt.NO_ARGUMENT,
		      "Display help", null);
	 cg.parse(asArgs);
	 
	 if (cg.hasOption('h')){
	     System.out.println(helpMessage);
	     return;}
	 boolean withPrompt = true;
	 if (cg.hasOption('x')){
	     withPrompt = false;}
	 // get Hostname and port number from params
	 int iPort = Integer.parseInt(cg.getValue('p'));
	 String sHost = cg.getValue('m');
	 if (sHost == null && withPrompt){
	   System.out.println("[Client] Warning: you did not specify a host");
	   sHost = "localhost";}
	 
	// create socket connecting to host at port
	 if (withPrompt){
	System.out.println("[Client] Connecting to \""
			   +sHost+"\" on Port "+iPort);}
	Socket sockTalk = new Socket(sHost, iPort);

      // get a buffered Reader to read from socket
      InputStream is        = sockTalk.getInputStream();
      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br     = new BufferedReader(isr);
      // get a printwriter to write to socket
      OutputStream os       = sockTalk.getOutputStream();
      PrintWriter pw        = new PrintWriter(os);
      
      // send message
      if (withPrompt){
	  System.out.println("[Client] Waiting for Server");}
      String sIn = br.readLine();
      if (!sIn.equals("!start!")){
	  System.out.println("[Client]Error: terminating");
	  sockTalk.close();
      }
      else{
	  if (withPrompt){
	  System.out.println("[Client] Lexicon ready");
	  System.out.println("[Client] Waiting for Input. Type \"finish!\" to terminate");}
	  BufferedReader stdIn = 
	      new BufferedReader(new InputStreamReader(System.in));
	  String userIn = stdIn.readLine();
	  while (!userIn.equals("finish!")){
	      if (withPrompt){
	      System.out.println("[Client] Sending message \""
				 +userIn+"\"");}
	      pw.println(userIn);
	      pw.flush();
	      if (withPrompt){
		  System.out.println("[Client] received Input: ");}
	      sIn = br.readLine();
	      while (!sIn.equals("!finish!")){
		  System.out.print(sIn+"\n");
		  sIn = br.readLine();
	      }
	      if (withPrompt){
		  System.out.println("[Client] Waiting for Input. Type \"finish!\" to terminate");}
	      userIn = stdIn.readLine();
	  }
	  if (withPrompt){
	  System.out.println("[Client] Sending message \""
	  		     +userIn+"\"");}
	  pw.println("!finish!");
	  pw.flush();
	  // terminating
	  if (withPrompt){
	      System.out.println("[Client] terminating");}
	  sockTalk.close();
      
      }
    }
    catch (UnknownHostException uhee) {
	System.out.println("[Client] Unknown host : "+asArgs[0]);
    } catch (IOException ioe) {
	System.out.println("[Client] Some IO Exception occurred");
    } catch (NumberFormatException nfe) {
	System.out.println(helpMessage);
    } catch (ArrayIndexOutOfBoundsException aioobe) {
	System.out.println(helpMessage);
    }
  }
}
