
package de.saar.coli.chorus.oracle;

import java.util.*;

public abstract class Oracle<SpaceType extends SearchSpace<DomainType>, DomainType> {
	protected SpaceType space;
	
	Oracle() {
		space = generateNewSpace();
	}
	
	public void run(int port) {
		while( true ) {
			Server serv = new Server(port);
			space = generateNewSpace();
			String messageStr;
			
			do {
				messageStr = serv.read();
				
				if( messageStr != null ) {
					Message m = new Message(messageStr);
					Message answer = processMessage(m);
					serv.write(answer);
				} 
			} while( messageStr != null );            
		}
	}
	
	
	protected SpaceType getSpace() {
		return space;
	}
	
	protected Message processMessage(Message m) {
		String type = m.getType();
		
		if( type.equals("init") ) {
			return new Message("confirm", null);
			
		} else if( type.equals("reset") ) {
			processReset();
			return new Message("confirm", null);
			
		} else if( type.equals("new") ) {
			String id = m.getArgument("id");
			String parentId = m.getArgument("parentid");
			
			if( id.equals(parentId) )
				parentId = null;
			
			DomainType dom = xmlToDomain(m.getData());
			
			space.addState( id, dom, parentId );
			
			return new Message("confirm", null);
			
		}
		
		else 
			return null;
		
	}
	
	// these two could always return the same object
	static protected Message rejectMessage() {
		return new Message("reject", null);
	}
	
	static protected Message confirmMessage() {
		return new Message("confirm", null);
	}
	
	
	// This method could in principle contain code for the init
	// message. It could also handle newState messages, except
	// that this again interferes with the type system.
	//    abstract protected Message processMessage(Message m);
	
	void processReset() {
		space = generateNewSpace();
	}
	
	abstract protected SpaceType generateNewSpace();
	
	abstract protected DomainType xmlToDomain(String elt);
	
	// todo: abstract protected Message parseXML()
}
