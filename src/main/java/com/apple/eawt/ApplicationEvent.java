package com.apple.eawt;

import java.io.Serializable;
import java.util.EventObject;

public class ApplicationEvent extends EventObject implements Serializable {
	public ApplicationEvent(Object arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
	
	public String getFilename() {
		return null;
	}
	
	public boolean isHandled() {
		return false;
	}
	
	public void setHandled(boolean x) {
		
	}
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
