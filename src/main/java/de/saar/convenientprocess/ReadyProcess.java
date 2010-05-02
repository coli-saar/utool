
package de.saar.convenientprocess;

/*
 This class is an interface to a process that outputs the line
 "-- Ready --" to stderr once it has finished its own initialization.
 The class represents the state of the subprocess, using the
 STATE_* constants.
 
 Other threads can wait() for the ReadyProcess object. Such threads
 will be notified once the subprocess state changes.
 
 Alexander Koller, Feb 2003
 */



public class ReadyProcess extends ConvenientProcess {
	private int state;
	
	public static final int STATE_UNINITIALIZED = 0;
	public static final int STATE_INITIALIZING = 1;
	public static final int STATE_READY = 2;
	public static final int STATE_FINISHED = 3;
	
	
	public ReadyProcess(String command) {
		super(command);
		
		state = STATE_INITIALIZING;
		myNotify();
	}
	
	public ReadyProcess(String command, String dir) {
		super(command, dir);
		
		state = STATE_INITIALIZING;
		myNotify();
	}
	
	public ReadyProcess(String[] cmdarray, String dir) {
		super(cmdarray, dir);
		
		state = STATE_INITIALIZING;
		myNotify();
	}
	
	
	
	public void run() {
		String line;
		boolean foundIt;
		
		foundIt = false;
		
		do {
			line = readLineStderr();
			
			if( line != null )
				if( line.indexOf("Ready") > -1 )
					foundIt = true;
		} while( !foundIt && (line != null) );
		
		state = STATE_READY;
		myNotify();
		
		waitForAndCleanup();
		state = STATE_FINISHED;
		myNotify();
	}
	
	public int getProcessState() {
		return state;
	}
	
	public String getStateText() {
		switch(state) {
		case STATE_UNINITIALIZED:
			return "UNITIALIZED";
		case STATE_INITIALIZING:
			return "INITIALIZING";
		case STATE_READY:
			return "READY";
		case STATE_FINISHED:
			return "FINISHED";
		default:
			return "(undefined)";
		}
	}
	
	public void waitForState(int targetState) {
		try {
			do {
				synchronized(this) {
					wait();
				}
			} while( targetState != getProcessState() );
		} catch(InterruptedException e) {
		}
	}
	
	private void myNotify() {
		//	System.err.println("state: " + state);
		
		synchronized(this) {
			notifyAll();
		}
	}
}
