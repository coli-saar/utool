package edu.mit.techniques.FOL;

import java.io.PrintStream;
import java.util.Vector;

public class ClauseList {

	// A clause list is a set of _clauses
	// Use a vector

	private Vector _clauses;


  /////
  // CONSTRUCTORS

	public ClauseList (){
		_clauses = new Vector();
	}

    public ClauseList (Clause c){
	_clauses = new Vector();
	addClause(c);
    }

	public ClauseList (Literal l){
		_clauses = new Vector();
		addClause(new Clause(l));
	}

	public ClauseList (Vector __clauses){
		_clauses = (Vector) __clauses.clone();
	}

	public Vector _clauses (){
		return _clauses;
	}

  public void addClause (Clause p){
      // If this clause is the empty clause, add this one and 
      // delete all other clauses
      if (p.size() == 0) {
	  _clauses = new Vector();
      } 

      // If it's the true clause, don't bother adding it.
      if (!p.isTrue()){
	  _clauses.addElement(p);
      }
  }

    // Return true if we contain the empty clause
    // If so, it should be the first (and only) clause

    public boolean isFalse(){
	return (_clauses.size() == 1 && 
		((Clause) _clauses.elementAt(0)).size() == 0);
    }

    // Return true if we contain no clauses
    
    public boolean isTrue(){
	return (_clauses.size() == 0);
    }
	

	public Clause clauseAt (int i){
		return (Clause) _clauses.elementAt(i);
	}

	public int size(){
		return _clauses.size();
	}

	// Don't side-effect this clause list
	public ClauseList concatenate (ClauseList c2){
		ClauseList c1 = new ClauseList(_clauses);
		for (int i = 0; i < c2.size(); i++){
			c1.addClause(c2.clauseAt(i));
		}
		return c1;
	}

	// Do side-effect this clause list!!
	public void nconc (ClauseList c2){
		for (int i = 0; i < c2.size(); i++){
			_clauses.addElement(c2.clauseAt(i));
		}
	}

	public String toString(){
		String result = "";
		for (int i = 0; i < _clauses.size(); i++){
			result = result + _clauses.elementAt(i).toString() + "\n";
		}
		return result;
	}


    // Change this to generate dimacs form?

    public void print (PrintStream ps){
	int numClauses = _clauses.size();
	
	ps.print("(");
	
	if (numClauses != 0) {
	    // Print the first one
	    ((Clause) _clauses.elementAt(0)).print(ps);
	    
	    // If there are more, print the rest, with "^" between
	    if (numClauses > 1) {
		for (int i = 1; i < numClauses; i++){
		    ps.print(" ^ ");
		    ((Clause) _clauses.elementAt(i)).print(ps);
		}
	    }
	}

	ps.print(")");
    }

}
