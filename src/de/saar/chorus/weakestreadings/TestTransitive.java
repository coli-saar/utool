
package de.saar.chorus.weakestreadings;

import edu.mit.techniques.FOL.*;
import edu.mit.techniques.FOL.parser.*;


class TestTransitive extends GenericTest {
    public void go(String[] args) {
	Entailment ent = Entailment.withName(args[1]);

	Sentence
	    axy = Parser.parse("a(x,z)"),
	    byz = Parser.parse("b(y,z)"),
	    czw = Parser.parse("c(y,x2)"),
	    dwv = Parser.parse("d(x1,x2)"),
	    ev  = Parser.parse("e(x1)");

	String x = "x", y = "y", z = "z", w = "x1", v = "x2";

	/*
	Rule cc = new Rule("aa",
			   Rule.a_l(y, z, axy, byz, Rule.a_l(w, v, czw, dwv, ev)),
			   Rule.a_r(y, z, axy, byz, Rule.a_r(w, v, czw, dwv, ev)));
	*/

	/*
	Rule cc = new Rule("bb",
			   Rule.b_l(y, z, axy, byz, Rule.b_l(w, v, czw, dwv, ev)),
			   Rule.b_r(y, z, axy, byz, Rule.b_r(w, v, czw, dwv, ev)));
	*/

	/*
	Rule cc = new Rule("cc",
			   Rule.c_l(y, z, axy, byz, Rule.c_l(w, v, czw, dwv, ev)),
			   Rule.c_r(y, z, axy, byz, Rule.c_r(w, v, czw, dwv, ev)));
	*/

	Rule cc = new Rule("bc",
			   Rule.b_l(y, z, axy, byz, Rule.c_l(w, v, czw, dwv, ev)),
			   Rule.b_r(y, z, axy, byz, Rule.c_r(w, v, czw, dwv, ev)));

	checkRuleContextEntailment(cc, "id", ent);


	Rule dd = new Rule("cb",
			   Rule.c_l(y, z, axy, byz, Rule.b_l(w, v, czw, dwv, ev)),
			   Rule.c_r(y, z, axy, byz, Rule.b_r(w, v, czw, dwv, ev)));
	checkRuleContextEntailment(dd, "id", ent);
    }


    TestTransitive(Boolean printCountermodels) {
	super(printCountermodels);
    }
}
