

package de.saar.convenientprocess;


/***
 * BUG: Java pipe streams have a limited capacity of 1000 characters.
 * This means that if we keep writing into the pipe without the other
 * end being read, the buffering thread will eventually block, and
 * no longer read the output from the process, which will then also
 * block eventually.
 *
 * This simply means that ConvenientProcess can't be used reliably
 * for now.
 *
 * AK 2.3.03
 */



/*
  Note:

  The BufferedReader returned by the getInputStream() doesn't
  return null when no more input from the process is available.
  Instead, it throws an IOException.

  TODO: This should be fixed at some later point.

  Alexander Koller, Feb 2003
*/

import java.io.*;
import java.util.*;

class InputBufferingThread extends Thread {
    private BufferedReader in;

    private Vector buffer;
    private boolean active;

    private PrintWriter inputLogStream;
    private boolean debug;


    InputBufferingThread(InputStream in) {
	super();

	this.in = new BufferedReader(new InputStreamReader(in));
	buffer = new Vector();
	active = true;

	inputLogStream = null;
	debug = false;
    }

    private void println(String s) {
	synchronized(buffer) {
	    buffer.add(s);
	    buffer.notifyAll();
	}
    }

    public boolean ready() {
	return !buffer.isEmpty();
    }

    public String readLine() {
	synchronized(buffer) {
	    try {
		if( buffer.isEmpty() ) {
		    if( !active )
			return null;

		    else
			buffer.wait();
		}
		
		return (String) buffer.remove(0);
	    } catch(InterruptedException e) {
		return null;
	    }
	}
    }


    public void run() {
	String line;

	try {
	    do {
		yield();
		line = in.readLine();

		if( debug )
		    System.err.println("-> " + line);
		

		if( inputLogStream != null ) {
		    inputLogStream.println(line);
		    inputLogStream.flush();
		}
				
		//		if( line != null ) {
		    println(line);
		    //		} 
	    } while( line != null );
	} catch (InterruptedIOException e) {
	    // This thread has been told to terminate. But finish
	    // reading first.

	    try {
		do {
		    line = in.readLine();
		    println(line);
		} while( line != null );
	    } catch(IOException e1) {
		e1.printStackTrace();
	    }
	} catch (IOException e) {
	    // Serious exceptions.

	    e.printStackTrace();
	} finally {
	    try {
		in.close();
	    } catch(IOException e) {
		e.printStackTrace();
	    }
	}
    }

    void setInputLog(PrintWriter pw) {
	inputLogStream = pw;
    }

    void setDebug(boolean d) {
	debug = d;
    }
}
	    
