
package de.saar.coli.theoremprovers.mace;


import java.util.Vector;

import nl.uu.smotterl.PrologOp;
import nl.uu.smotterl.PrologTokenizer;
import nl.uu.smotterl.Term;


class MaceParser {
	public static Model parse(String s) {
		String cleanedUp = s.replace('$', 's');
		
		// parse the prolog Term
		PrologOp.makeops();
		PrologTokenizer tok = new PrologTokenizer(cleanedUp);
		Term t = tok.gettermdot(null);
		
		//	show(t,"");
		
		
		
		
		// now build the model representation
		Model ret = new Model();
		
		ret.setSize(t.getArg(0).getArity());
		
		
		Vector symbols = parseList(t.getArg(1));
		for( int i = 0; i < symbols.size(); i++ ) {
			Term sym = (Term) symbols.get(i);
			String label = sym.getArg(0).getName();
			int arity = sym.getArg(0).getArity();
			Vector tableVec = parseList(sym.getArg(1));
			
			int[] table = new int[tableVec.size()];
			for( int j = 0; j < tableVec.size(); j++ )
				table[j] = ((Term)tableVec.get(j)).getArity();
			
			//	    System.err.println(label + "/" + arity + " is a -" + sym.getName() + "-");
			
			if( sym.getName().equals("predicate") )
				ret.addPredicate(label, arity, table);
			else
				ret.addFunction(label, arity, table);
		}
		
		return ret;
	}
	
	private static void show(Term t, String indent) {
		if( t.getType() == Term.FUNCTOR ) {
			
			if( t.getName().equals(".") ) {
				System.out.println(indent + "list [ ");
				Vector x = parseList(t);
				for( int i = 0; i < x.size(); i++ )
					show((Term) x.get(i), indent + "  ");
				System.out.println(indent + "]");
			}
			
			else {
				System.out.println(indent + "fct " + t.getName() + " ( ");
				for(int i = 0; i < t.getArity(); i++ )
					show(t.getArg(i), indent + "  ");
				System.out.println(indent + ") fct " + t.getName());
			}
		} else if( t.getType() == Term.NUMBER ) {
			System.out.println(indent + "num " + t.getArity());
		} else {
			System.out.println(indent + "atom " + t.getName());
		}
	}
	
	private static Vector parseList(Term t) {
		Vector ret = new Vector();
		
		while( (t.getType() == Term.FUNCTOR) && t.getName().equals(".") ) {
			ret.add(t.getArg(0));
			t = t.getArg(1);
		}
		
		
		return ret;
	}
	
	
	// testing
	public static void main(String args[]) {
		parse("interpretation( 2, [\n\n        predicate(p(_,_,_), [\n                1, 0,\n                0, 0,\n\n                0, 0,\n                0, 0   ]),\n\n        function($c3, [0]),\n        function($c2, [0]),\n        function($c1, [0]),\n        function(a, [0]),\n        function(b, [0]),\n        function(c, [1])\n  ]).\n");
	}
}
