
package de.saar.chorus.weakestreadings;

import edu.mit.techniques.FOL.*;
import edu.mit.techniques.FOL.parser.*;


class Rule {



    // methods insert formulas into LHS and RHS of rules

    static Sentence a_l(String y, String z, Sentence A, Sentence B, Sentence C) {
	Variable Y = new Variable(y), Z = new Variable(z);

	return Auxiliary.exists(Y, Auxiliary.forall(Z, A, B), C);
    }

    static Sentence a_r(String y, String z, Sentence A, Sentence B, Sentence C) {
	Variable Y = new Variable(y), Z = new Variable(z);

	return Auxiliary.forall(Z, A, Auxiliary.exists(Y, B, C));
    }

    static Sentence b_l(String y, String z, Sentence A, Sentence B, Sentence C) {
	Variable Y = new Variable(y), Z = new Variable(z);

	return Auxiliary.forall(Y, Auxiliary.exists(Z, A, B), C);
    }

    static Sentence b_r(String y, String z, Sentence A, Sentence B, Sentence C) {
	Variable Y = new Variable(y), Z = new Variable(z);

	return Auxiliary.exists(Z, A, Auxiliary.forall(Y, B, C));
    }

    static Sentence c_l(String y, String z, Sentence A, Sentence B, Sentence C) {
	Variable Y = new Variable(y), Z = new Variable(z);

	return Auxiliary.forall(Z, A, Auxiliary.forall(Y, B, C));
    }

    static Sentence c_r(String y, String z, Sentence A, Sentence B, Sentence C) {
	Variable Y = new Variable(y), Z = new Variable(z);

	return Auxiliary.forall(Y, Auxiliary.forall(Z, A, B), C);
    }
    

    static Sentence
	axz = Parser.parse("a(x,z)"),
	byz = Parser.parse("b(y,z)"),
	cy = Parser.parse("c(y)");


    // rewriting rules

    static Rule 
	a = new Rule("a", a_l("y", "z", axz, byz, cy), a_r("y", "z", axz, byz, cy)),
	b = new Rule("b", b_l("y", "z", axz, byz, cy), b_r("y", "z", axz, byz, cy)),
	c = new Rule("c", c_l("y", "z", axz, byz, cy), c_r("y", "z", axz, byz, cy));
			     


    /*
    static Rule 
	a = new Rule("a",
		     Parser.parse("exists y ((all z (a(x,z) -> b(y,z))) ^ c(y))"),
		     Parser.parse("all z (a(x,z) -> exists y (b(y,z) ^ c(y)))")),
	b = new Rule("b",
		     Parser.parse("all y ((exists z (a(x,z) ^ b(z,y))) -> c(y))"),
		     Parser.parse("exists z (a(x,z) ^ all y (b(z,y) -> c(y)))")),
	c = new Rule("c",
		     Parser.parse("all z (a(x,z) -> (all y (b(z,y) -> c(y))))"),
		     Parser.parse("all y ((all z (a(x,z) -> b(z,y))) -> c(y))"));
    */

    static Rule[] rules = { a, b, c };




    private Sentence lhs, rhs;
    private String label;

    Rule(String label, Sentence lhs, Sentence rhs) {
	this.label = label;
	this.lhs = lhs;
	this.rhs = rhs;
    }

    Sentence getLHS() {
	return lhs;
    }

    Sentence getRHS() {
	return rhs;
    }

    String getLabel() {
	return label;
    }

    Rule invert() {
	return new Rule(label + " (inv)", rhs, lhs);
    }
}
