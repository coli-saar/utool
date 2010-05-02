
package de.saar.chorus.weakestreadings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


class TestTransitiveShell extends GenericTest {
    public void go(String[] args) {
	Entailment ent = Entailment.withName(args[1]);

	Entailment ppp = Entailment.withName("piPlusPlus");

	String[] contexts = Contexts.available();

	BufferedReader in
	    = new BufferedReader(new InputStreamReader(System.in));

	try {
	    while(true) {
		System.out.print("\n\nLHS formula: ");
		String lhs = in.readLine();

		if( lhs == null ) {
		    System.out.println("");
		    System.exit(0);
		}
		
		System.out.print("RHS formula: ");
		String rhs = in.readLine();


		System.out.println("\nLHS: " + SimpleFormula.parse(lhs));
		System.out.println("\nRHS: " + SimpleFormula.parse(rhs));
		
		Rule r = new Rule("human input",
				  SimpleFormula.parse(lhs).toSentence(),
				  SimpleFormula.parse(rhs).toSentence());

		System.out.println("\n" + ent.getLabel() + ":");
		checkRuleContextEntailment(r, "id", ent);

		System.out.println("\n" + ppp.getLabel() + ":");
		checkRuleContextEntailment(r, "id", ppp);
		
	    }
	} catch(IOException e) {
	}
    }



    TestTransitiveShell(Boolean printCountermodels) {
	super(printCountermodels);
    }
}


/*

Rules:

(a)    e(a,.)      ->  a(.,e)
(b)    a(e,.)      ->  e(.,a)
(c)    a(.,a)      ->  a(a,.)

(aa)   e(a,e(a,.)) ->  a(.,e(.,a(.,e)))

(bbb)  a(e,a(e,a(e,.))) -> e(.,a(.,e(.,a(.,e(.,a)))))
(bcbc) a(e,a(.,a(.,a(e,a(.,a))))) -> e(.,a(.,a(a,e(.,a(.,a(a,.))))))




Kombinationen von Regeln mit sich selbst, in verschiedenen Positionen:


                          piPol            pi (links muss eingebettet sein:)

la(ra,.,.) -> ra(la,.,.)  ok               ra
la(.,la,.) -> ra(.,ra,.)  ok               la
la(.,.,la) -> ra(.,.,ra)  ok               la

lb(?,.,.)  -> rb(?,.,.)   NICHT OK         lb ODER rb!
                   -> projectPol geht!! fuer lb(lb) -> rb(rb)
lb(.,rb,.) -> rb(.,lb,.)  ok               rb
lb(.,.,lb) -> rb(.,.,rb)  ok               lb

lc(lc,.,.) -> rc(rc,.,.)  ok               lc ODER rc!
lc(.,rc,.) -> rc(.,lc,.)  ok               rc
lc(.,.,lc) -> rc(.,.,rc)  ok               lc


NB:

lb(lc,.,.) -> rb(rc,.,.)  ok!
lb(la,.,.) -> rb(ra,.,.)  ok!
lc(lb,.,.) -> lc(rb,.,.)  ok!


TODO: _Warum_ ist gerade die Kombination von lb/lb so viel problematischer als 
alles andere?



ABER:
-----

Weder lb(lb,.,.) -> rb(rb,.,.) noch lb(rb,.,.) -> rb(lb,.,.)
 -- denn das Auftreten von A wechselt bei Anwendung von b seine Polaritaet

Dieses Beispiel ist auch ein problematisches kritisches Paar. Der tiefere
Redex aendert seine Polaritaet bei Reduktion des aeusseren Redex. In einer
Rewrite-Reihenfolge kann man 2x reduzieren, bei der anderen nur einmal.

Es folgt leider: Reduzierbarkeit von nicht-ueberlappenden Redexen ist
(anders als bei normalem Rewriting) _nicht_ voneinander unabhaengig. :( :(




UEBERLAPPENDE REDEXE
--------------------

a/a: e(la,.)             -> ra(.,e,.)               ok
a/b: la(e,.,.)           -> rb(.,.,e)               ok
a/c: a(.,la)             -> rc(.,.,e)               ok

b/b: a(lb,.)             -> rb(.,a,.)               NICHT OK
-- soll auch nicht, da 1. Schritt in neg. Polaritaet verstaerkt!

b/c: lb(.,.,a)           -> e(.,rc)                 ok

c/c: lc(.,.,a)           -> a(rc,.)                 ok

c a-: lc(.,e,.)          -> a(la,.)                 ok



*/
