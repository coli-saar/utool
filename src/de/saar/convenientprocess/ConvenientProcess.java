
package de.saar.convenientprocess;

/*
  This is a convenience wrapper for a process, which has the following
  advantages over the ordinary Process class:

   - Don't need to fiddle with Runtime; just pass the command
     to the constructor.

   - Input and output streams have more plausible names.

   - Don't need to worry about reading from the process's
     streams regularly; the input is buffered.

  Restrictions:

   - only works for processes that produce line-based output
     (inherit this restriction from InputBufferingThread)

  TODO:

   - Do something sensible if starting the process failed.

  Alexander Koller, Feb 2003
*/


import java.io.*;

public class ConvenientProcess extends Thread {
    protected Process process;
    private OutputStream in;

    protected InputBufferingThread bOut, bErr;

    private PrintWriter outputLogStream, inputLogStream;
    private boolean debug;


    public ConvenientProcess(String cmd) {
	try {
	    process = Runtime.getRuntime().exec(cmd);
	} catch(IOException e) {
	    e.printStackTrace();
	}

	inputLogStream = outputLogStream = null;
	debug = false;

	in = process.getOutputStream(); // process's stdin
	startupThreads();

    }

    public ConvenientProcess(String cmd, String dir) {
	try {
	    process = Runtime.getRuntime().exec(cmd, null, new File(dir));
	} catch(IOException e) {
	    e.printStackTrace();
	}

	inputLogStream = outputLogStream = null;
	debug = false;

	in = process.getOutputStream(); // process's stdin
	startupThreads();
    }

    public ConvenientProcess(String[] cmdarray, String dir) {
	try {
	    process = Runtime.getRuntime().exec(cmdarray, null, new File(dir));
	} catch(IOException e) {
	    e.printStackTrace();
	}

	inputLogStream = outputLogStream = null;
	debug = false;

	in = process.getOutputStream(); // process's stdin
	startupThreads();
    }
	

    public void run() {
	waitForAndCleanup();
    }

    public OutputStream stdin() {
	return in;
    }

    public void println(String s) {
	if( debug )
	    System.err.println("<- " + s);

	if( outputLogStream != null ) {
	    outputLogStream.println(s);
	    outputLogStream.flush();
	}

	(new PrintWriter(stdin(), true)).println(s);
    }

    public void closeWritingPipe() {
	try {
	    stdin().close();

	    if( debug )
		System.err.println("Writing pipe closed.");

	    if( outputLogStream != null )
		outputLogStream.close();
	} catch(IOException e) {
	    // TODO: Something
	}
    }

    public String readLineStdout() {
	return bOut.readLine();
    }

    public String readLineStderr() {
	return bErr.readLine();
    }

    public boolean readyStdout() {
	return bOut.ready();
    }

    public boolean readyStderr() {
	return bErr.ready();
    }

    public void destroy() {
	process.destroy();
    }

    public int exitValue() {
	return process.exitValue();
    }


    public void setOutputLog(String filename) {
	try {
	    outputLogStream = new PrintWriter(new FileOutputStream(filename, false), true);
	} catch(IOException e) {
	    e.printStackTrace();
	}
    }

    public void setInputLog(String filename) {
	try {
	    inputLogStream = new PrintWriter(new FileOutputStream(filename, false), true);
	    bOut.setInputLog(inputLogStream);
	    bErr.setInputLog(inputLogStream);
	} catch(IOException e) {
	    e.printStackTrace();
	}
    }

    public void setDebug(boolean debug) {
	this.debug = debug;

	bOut.setDebug(debug);
	bErr.setDebug(debug);
    }


    protected void waitForAndCleanup() {
	try {
	    process.waitFor();
	} catch(InterruptedException e) {
	} finally {
	    cleanupStreams();
	}
    }

    protected void startupThreads() {
	bOut = new InputBufferingThread(process.getInputStream());
	bErr = new InputBufferingThread(process.getErrorStream());
	bOut.start();
	bErr.start();
    }	

    protected void cleanupStreams() {
	if( bOut != null ) bOut.interrupt();
	if( bErr != null ) bErr.interrupt();
	try {
	    in.close();
	} catch(IOException e) {
	    e.printStackTrace();
	}
    }
}
