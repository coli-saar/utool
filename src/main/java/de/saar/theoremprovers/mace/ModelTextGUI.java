
// TODO: Predicate tables should print values T/F instead of 1/0.

package de.saar.theoremprovers.mace;


import edu.mit.techniques.FOL.Sentence;
import edu.mit.techniques.FOL.parser.Parser;




public class ModelTextGUI {
    private Model model;

    ModelTextGUI(Model m) {
	this.model = m;
    }


    public String toString() {
	String ret = "----------------------------------------------\nModel with " + model.getSize() + " individuals\n----------------------------------------------\n";

	ModelTable[] 
	    predicates = model.getPredicates(),
	    functions  = model.getFunctions();

	for( int i = 0; i < functions.length; i++ )
	    ret += tableToString(functions[i]);

	for( int i = 0; i < predicates.length; i++ )
	    ret += tableToString(predicates[i]);

	return ret + "----------------------------------------------\n";
    }

    public void display(java.io.PrintStream o) {
	o.println(toString());
    }

    public static void displayModel(java.io.PrintStream o, Model m) {
	new ModelTextGUI(m).display(o);
    }


    private String tableToString(ModelTable table) {
	String ret = table.getLabel() + ": ";

	switch(table.getArity()) {
	case 0: 
	    return ret + table.getTable()[0] + "\n";

	case 1:
	    //	    ret += "/" + table.getArity();
	    ret += "    ";
	    for( int i = 0; i < model.getSize(); i++ )
		ret += i + " ";
	    ret += "\n";

	    ret += "   ---";
	    for( int i = 0; i < model.getSize(); i++ )
		ret += "--";
	    ret += "\n";

	    ret += "       " + printTableLine(table, 0);

	    return ret + "\n\n";


	case 2:
	    // Interpretations of arity 2 are printed as 
	    // two-dimensional tables. The value of A(x,y)
	    // is in row x, column y.

	    // header
	    //	    ret += "/" + table.getArity();
	    ret += "  | ";
	    for( int i = 0; i < model.getSize(); i++ )
		ret += i + " ";
	    ret += "\n";
	
	    ret += "   --+";
	    for( int i = 0; i < model.getSize(); i++ )
		ret += "--";
	    ret += "\n";

	    // now each line
	    for( int i = 0; i < model.getSize(); i++ ) {
		ret += "   " + i + " | ";
		ret += printTableLine(table, i);
		ret += "\n";
	    }

	    return ret + "\n";

	default:
	    return ret + "(arity > 2 not yet implemented)\n";
	}
    }

    private String printTableLine(ModelTable table, int row) {
	String ret = "";

	for( int j = 0; j < model.getSize(); j++ )
	    ret += table.getTableEntry(row, j) + " ";
    
	return ret;
    }


    public static void main(String[] args) {
	String test = "p(a) ^ p(b)";
	Sentence[] s = new Sentence[1];
	s[0] = Parser.parse(test);

	Model m = Mace.computeModel("Test", s);

	displayModel(System.err, m);
    }

}
