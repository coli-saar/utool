
package de.saar.coli.chorus.weakestreadings;

import edu.mit.techniques.FOL.*;
import edu.mit.techniques.FOL.parser.*;


class Contexts {


    // The following contexts all bind the variable x.
    static Sentence as(Sentence sent) {
	Variable x = new Variable("x");

	return new Universal(x,
			     new Implication(Parser.parse("d(x)"),
					     sent));
    }

    static Sentence ar(Sentence sent) {
	Variable x = new Variable("x");

	return new Universal(x,
			     new Implication(sent,
					     Parser.parse("d(x)")));
    }
							     
    static Sentence e(Sentence sent) {
	Variable x = new Variable("x");

	return new Existential(x,
			       new Conjunction(Parser.parse("d(x)"),
					       sent));
    }


    // The following contexts all bind the variable x and introduce
    // a new variable x1.
    static Sentence asx(Sentence sent) {
	Variable x = new Variable("x");

	return new Universal(x,
			     new Implication(Parser.parse("d(x1,x)"),
					     sent));
    }

    static Sentence arx(Sentence sent) {
	Variable x = new Variable("x");

	return new Universal(x,
			     new Implication(sent, Parser.parse("d(x1,x)")));
    }
							     
    static Sentence ex(Sentence sent) {
	Variable x = new Variable("x");

	return new Existential(x,
			       new Conjunction(Parser.parse("d(x1,x)"),
					       sent));
    }


    // The empty context
    static Sentence id(Sentence sent) {
	return sent;
    }




    static Sentence insert(Sentence sent, String context) {
	if( context.equals("as") )
	    return as(sent);

	else if( context.equals("ar") )
	    return ar(sent);

	else if( context.equals("e") )
	    return e(sent);

	if( context.equals("asx") )
	    return asx(sent);

	else if( context.equals("arx") )
	    return arx(sent);

	else if( context.equals("ex") )
	    return ex(sent);

	else
	    return id(sent);
    }

    static String[] available() {
	String[] ret = {"id", "as", "ar", "e"};
	return ret;
    }

    static boolean negative(String contextname) {
	if( contextname.equals("ar") )
	    return true;
	else if( contextname.equals("arx") )
	    return true;
	else return false;
    }

}
