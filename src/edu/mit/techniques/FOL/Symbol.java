package edu.mit.techniques.FOL;

public class Symbol implements Cloneable {

	private static int counter = 0;
	private static String prefix = "F_";

	private String name;

	public Symbol (String _name){
		name = _name;
	}

	// Make a unique new name
	public Symbol (){
		counter++;
		name = prefix + counter;
	}

	public Object clone(){
		try{
			return super.clone();
		}
		catch(CloneNotSupportedException e){ // won't happen
			return null;
		}
	}

	public String name(){
		return name;
	}

	public boolean equals(Object o) {
		if (o != null)
			if (o instanceof Symbol)
				return name.equals(((Symbol)o).name);
			else
				return false;
		else
			return false;
	} // end of METHOD equals

	public String toString(){
		return name;
	}

}
