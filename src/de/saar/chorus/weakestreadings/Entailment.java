

package de.saar.chorus.weakestreadings;

import edu.mit.techniques.FOL.*;
import edu.mit.techniques.FOL.parser.*;

import java.util.*;
import java.lang.reflect.*;


abstract class Entailment {
    private String label;

    //    abstract Sentence[] global(Sentence l, Sentence r);
    abstract Sentence[] lhs(Sentence l, Sentence r);
    abstract Sentence[] rhs(Sentence l, Sentence r);

    Entailment(String l) {
	label = l;
    }

    String getLabel() {
	return label;
    }

    Sentence makeImplication(Sentence l, Sentence r) {
	return new Implication(Auxiliary.listAnd(lhs(l,r)),
			       Auxiliary.listAnd(rhs(l,r)));
    }

    

    // The entailments that are defined.

    /*

Problems = unit(pi:unit(label:'Altes Pi-Entailment'
			global:fun {$ A B} nil end
			lhs:fun {$ A B} [A {Pi A}] end
			rhs:fun {$ A B} [B {Pi B}] end)

		lpi:unit(label:'Beide Pis auf der linken Seite'
			 global: fun {$ A B} [{Pi A} {Pi B}] end
			 lhs:fun {$ A B} [A] end
			 rhs:fun {$ A B} [B] end)

		star:unit(label:'*-Entailment'
			  global: fun {$ A B} nil end
			  lhs: fun {$ A B} [{ExistentialVersion A} A] end
			  rhs: fun {$ A B} [{ExistentialVersion B} B] end)

		pure:unit(label:'Ohne Praesuppositionen'
			  global: fun {$ A B} nil end
			  lhs: fun {$ A B} [A] end
			  rhs: fun {$ A B} [B] end))
    */

    public static Entailment pure = new Entailment("Ohne Praesuppositionen") {
	    /*	    Sentence[] global(Sentence l, Sentence r) {
		return new Sentence[0];
	    }
	    */

	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence[] ret = {l};
		return ret;
	    }

	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {r};
		return ret;
	    }
	};

    public static Entailment star = new Entailment("*-Entailment") {
	    /*	    Sentence[] global(Sentence l, Sentence r) {
		return new Sentence[0];
		} */

	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence[] ret = {l, Presuppositions.existentialVersion(l)};

		return ret;
	    }

	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {r, Presuppositions.existentialVersion(r)};
		return ret;
	    }
	};

    public static Entailment altesPi = new Entailment("Transitives Pi-Entailment") {
	    /*	    Sentence[] global(Sentence l, Sentence r) {
		return new Sentence[0];
		}*/

	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence[] ret = {l, Presuppositions.pi(l)};
		return ret;
	    }

	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {r, Presuppositions.pi(r)};
		return ret;
	    }
	};


    public static Entailment piB = new Entailment("A mit pi(B)") {
	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence[] ret = {l, Presuppositions.pi(r)};
		return ret;
	    }

	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {r};
		return ret;
	    }
	};


    public static Entailment pi = new Entailment("Beide Pis auf der linken Seite") {
	    /*	    Sentence[] global(Sentence l, Sentence r) {
		Sentence[] ret = {Presuppositions.pi(l), Presuppositions.pi(r)};
		
		return ret;
		}*/
	    
	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence[] ret = {l, Presuppositions.pi(l), Presuppositions.pi(r)};
		return ret;
	    }
	    
	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {r};
		return ret;
	    }
	};


    public static Entailment piUndSchlange = new Entailment("Schlange(A) & pi(B) |= B") {
	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence[] ret = {Presuppositions.piip2(l), 
				  Presuppositions.pi(r)};
		return ret;
	    }
	    
	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {r};
		return ret;
	    }
	};


    public static Entailment piInPlace = new Entailment("Beide Pis auf der linken Seite, LHS inPlace") {
	    /*
	    Sentence[] global(Sentence l, Sentence r) {
		//Sentence[] ret = {Presuppositions.pi(l), Presuppositions.pi(r)};
		//		Sentence[] ret = {Presuppositions.pi(r)};
		
		//	return ret;
		return new Sentence[0];
	    }
	    */
	    
	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence[] ret = {//Presuppositions.pi(l), Presuppositions.pi(r), 
				  Presuppositions.piip2(l)};
		return ret;
	    }
	    
	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {Presuppositions.piip2(r)};
		//Sentence[] ret = {r};
		return ret;
	    }
	};


    public static Entailment piipq = new Entailment("Links verst., rechts absch. PiIP") {
	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence[] ret = {//Presuppositions.pi(l), Presuppositions.pi(r), 
				  Presuppositions.piip2(l)};
		return ret;
	    }
	    
	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {Presuppositions.piiq2(r)};
		//Sentence[] ret = {r};
		return ret;
	    }
	};



    public static Entailment projectq = new Entailment("Links verst., rechts absch. Project") {
	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence[] ret = {//Presuppositions.pi(l), Presuppositions.pi(r), 
				  Presuppositions.projected(l)};
		return ret;
	    }
	    
	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {Presuppositions.projectedImp(r)};
		//Sentence[] ret = {r};
		return ret;
	    }
	};


   public static Entailment pi2InPlace = new Entailment("piip2(A) -> B") {
	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence piip = Presuppositions.piip2(l);

		//		System.err.println("Sentence: " + l);
		//		System.err.println("With piip2: " + piip);

		Sentence[] ret = {piip};
		return ret;
	    }
	    
	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {r};
		return ret;
	    }
	};
    
    public static Entailment piPol = new Entailment("piipp(A,+) -> piipp(B,-)") {
	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence piip = Presuppositions.piipPol(l, true);

		Sentence[] ret = {piip};
		return ret;
	    }
	    
	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {Presuppositions.piipPol(r,false)};
		return ret;
	    }
	};

    public static Entailment piPlusPlus = new Entailment("piipp(A,+) -> piipp(B,+)") {
	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence piip = Presuppositions.piipPol(l, true);

		Sentence[] ret = {piip};
		return ret;
	    }
	    
	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {Presuppositions.piipPol(r,true)};
		return ret;
	    }
	};


    public static Entailment piMinusMinus = new Entailment("piipp(A,-) -> piipp(B,-)") {
	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence piip = Presuppositions.piipPol(l, false);

		Sentence[] ret = {piip};
		return ret;
	    }
	    
	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {Presuppositions.piipPol(r,false)};
		return ret;
	    }
	};

    public static Entailment piipTransitive = new Entailment("Pi in place, transitiv") {
	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence[] ret = {Presuppositions.piip2(l)};
		return ret;
	    }
	    
	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {Presuppositions.piip2(r)};
		//Sentence[] ret = {r};
		return ret;
	    }
	};


    public static Entailment exA = new Entailment("Nur exists(z A)") {
	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence[] ret = {l, Parser.parse("exists z a(x,z)")};
		return ret;
	    }
	    
	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {r, Parser.parse("exists z a(x,z)")};
		return ret;
	    }
	};


    public static Entailment project = new Entailment("Mit Projektion") {
	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence[] ret = {Presuppositions.projected(l)};
		return ret;
	    }
	    
	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {r};
		return ret;
	    }
	};

    public static Entailment projectPol = new Entailment("Proj(A,+) -> Proj(B,-)") {
	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence[] ret = {Presuppositions.projectedPol(l, true)};
		return ret;
	    }
	    
	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {Presuppositions.projectedPol(r, false)};
		return ret;
	    }
	};


    public static Entailment projectPolPatched = new Entailment("Proj(A,+) -> Proj(B,-) (geflickt)") {
	    Sentence[] lhs(Sentence l, Sentence r) {
		Sentence[] ret = {Presuppositions.projectedPolPatched(l, true)};
		return ret;
	    }
	    
	    Sentence[] rhs(Sentence l, Sentence r) {
		Sentence[] ret = {Presuppositions.projectedPolPatched(r, false)};
		return ret;
	    }
	};



    public static Entailment[] entailments = { pi, altesPi, pure, star, piInPlace };


    public static Entailment withName(String name) {
	try {
	    Class ent = Entailment.class;
	    Field field = ent.getDeclaredField(name); 
	    return (Entailment) field.get(null);
	} catch(Exception e) {
	    return null;
	}
    }	
}
