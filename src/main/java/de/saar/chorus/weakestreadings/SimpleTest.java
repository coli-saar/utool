

package de.saar.chorus.weakestreadings;





class SimpleTest {
    public static void main(String args[]) {
	/*	SimpleFormula fmla = new SimpleExists(1, new SimpleAll(2),
					      new SimpleExists(3, new SimpleAll(4),
							       new SimpleAtom()));
	*/

	SimpleFormula fmla = SimpleFormula.parse("a(e,a(e,.))");

	System.err.println(fmla);
    }
}

