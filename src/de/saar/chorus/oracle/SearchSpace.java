
package de.saar.coli.chorus.oracle;

import java.util.*;
import java.lang.reflect.*;


public class SearchSpace<DomainType> {
    protected Map<String, DomainType> states;
    protected Map<String, String> parents;

    public SearchSpace() {
	states = new HashMap<String, DomainType>();
	parents = new HashMap<String, String>();
    }
    
    public DomainType getStateForName(String name) {
	return states.get(name);
    }

    public String getRootName() {
	return "root";
    }

    public boolean isEmpty() {
	return states.isEmpty();
    }

    /** 
     * 
     * 
     * @param stateName 
     * 
     * @return the name of the parent state; null if the state is the root.
     */    
    public String getParentName(String stateName) {
	return parents.get(stateName);
    }

    // root state gets parentName == null
    public void addState(String id, DomainType contents, String parentId) {
	states.put(id, contents);
	
	if( parentId != null )
	    parents.put(id, parentId);
    }

    public List<String> getChildren(String parentName) { 
	List<String> l = new LinkedList<String>();

	for( String stateName : states.keySet() ) 
	    // use parentName.equals because getParentName() could be null
	    if( parentName.equals(getParentName(stateName)) )
		l.add(stateName);

	return l;
    }

}






    /* ** das funktioniert nicht -- die JVM findet den Konstruktor nicht

    public StateType newState(String newName, String parent, DomainType info) {
	StateType parentState = getStateForName(parent);
	StateType 

	try {
	    Class actualType = parentState.getClass();
	    System.err.println("actual: " + actualType);
	    Constructor con = 
		actualType.getConstructor(new Class[] {String.class,
						       parentState.getContent().getClass() });
	    System.err.println("constr: " + con);
	    StateType newState = 
		(StateType) con.newInstance( newName, info );

	    newState.setSpace(this);
	    states.put(newName, newState);

	    return newState;
	} catch(Exception e) {
	    e.printStackTrace(System.err);
	    return null;
	}
    }
    */
