

package de.saar.theoremprovers.mace;


import de.saar.convenientprocess.ConvenientProcess;
import edu.mit.techniques.FOL.Negation;
import edu.mit.techniques.FOL.Sentence;



public class Mace {
    private ConvenientProcess process;

    private String foundModel;
    private boolean outputProcessed;

    private String desc;

    private static int problemcounter = 1;


    public Mace(String desc, String logfile) {
	String cmdname = System.getProperty("mace.bin");

	//	System.err.println("Mace logfile: " + logfile);

	process = new ConvenientProcess(cmdname + " -c -P -t2 -n2 -N20");
	//	System.err.println("Mace started.");
	process.setOutputLog(logfile + ".toMace");
	process.setInputLog(logfile + ".fromMace");
	//	process.setDebug(true);

	foundModel = null;
	outputProcessed = false;

	this.desc = desc;
    }

    public static Model computeModel(String desc, Sentence[] sents) {
	String logfile = "logs/chorusprob." + problemcounter;
	String realdesc;

	if( desc == null )
	    realdesc = "chorusprob." + problemcounter;
	else
	    realdesc = desc;

	// For some reason, I have to wait for a few milliseconds here
	// before starting the Mace process. Otherwise, a new Mace process
	// will be started, and everything will be sent off ok to it, but
	// it sometimes doesn't do anything at all.
	//
	// With a delay of 20ms, it _mostly_ works, but not always. In those
	// cases, simply restart the program.
	try {
	    Thread.sleep(50);
	} catch(Exception e) { }

	Mace m = new Mace(desc, logfile);

	problemcounter++;

	m.send(sents);
	
	if( m.findModel() == null )
	    return null;

	else
	    return MaceParser.parse(m.findModel());
    }

    public static Model computeCountermodel(String desc, Sentence[] axioms, Sentence theorem) {
	//	System.err.println("mace cm: " + theorem);

	Sentence[] sents = new Sentence[axioms.length + 1];

	for( int i = 0; i < axioms.length; i++ )
	    sents[i] = axioms[i];
	sents[axioms.length] = new Negation(theorem);

	return computeModel(desc, sents);
    }


    private void send(Sentence[] sents) {
	process.println("% Problem: " + desc);

	process.println("set(auto).");
	process.println("formula_list(usable).");
	
	for( int i = 0; i < sents.length; i++ )
	    process.println(sents[i].toMace() + ".");

	process.println("end_of_list.");
	process.closeWritingPipe();

	process.run();
    }

    // call this after process.run() has terminated.
    private String findModel() {
	String ret = "", line;

	if( outputProcessed ) {
	    return foundModel;
	} else {
	    do {
		line = process.readLineStdout();
		if( line == null )
		    break;
	    } while ( line.indexOf("=== Model #1") < 0 );
	    
	    if( line == null ) {
		outputProcessed = true;
		return null;
	    }
	    
	    else {
		do {
		    line = process.readLineStdout();
		    ret = ret + "\n" + line;
		} while( line.indexOf("]).") < 0 );
		
		foundModel = ret;
		outputProcessed = true;

		return ret;
	    }
	}
    }
	
    /*
    public static void main(String[] args) {
	Mace m = new Mace();


	String test = "p(a) ^ p(b)";
	Sentence[] s = new Sentence[1];

	s[0] = Parser.parse(test);
	m.send(s);

	System.err.println("model: " + m.findModel());
	System.err.println("model: " + m.findModel());

	Model model = MaceParser.parse(m.findModel());
	System.out.println("model: " + model);
    }
    */

}
