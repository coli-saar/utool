package edu.mit.techniques.FOL;


abstract public class Connective extends Sentence {

	Sentence s1;
	Sentence s2;

	public Connective(Sentence s1, Sentence s2){
		this.s1 = s1;
		this.s2 = s2;
	}

	public Object clone(){
		Connective c = (Connective) super.clone();
		c.s1 = (Sentence)s1.clone();
		c.s2 = (Sentence)s2.clone();
		return c;
	}

    
    public Sentence[] subformulas() {
	Sentence[] ret = {s1, s2};
	return ret;
    }

    public void setSubformula(int i, Sentence s) {
	if( i == 0 )
	    s1 = s;
	else
	    s2 = s;
    }

}
