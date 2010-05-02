
package de.saar.chorus.oracle.sxdg;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import de.saar.chorus.oracle.EvaluatingOracle;
import de.saar.chorus.oracle.Evaluator;
import de.saar.chorus.oracle.Message;
import de.saar.chorus.oracle.SortedEvaluatingSearchSpace;


/* 
 
 Test:
 
 <new id='root' parentid='root' desc='rootstr' />
 <confirm />
 <new id='child1' parentid='root' desc='child1str' />
 <confirm />
 <minGlobal />
 <minGlobalResult value="1.0" id="root" />
 <minLocal parentid='root' />
 <minLocalResult value="1.0" childid="child1" />
 
 */



public class Main extends EvaluatingOracle<SortedEvaluatingSearchSpace<SxdgReflection>,SxdgReflection> {
	private Evaluator<SxdgReflection> eval;
	private String parent;
	
	
	private static void usage() {
		System.err.println("Usage: java -jar SxdgOracle.jar [options] <probability-file-name.xml>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -p <port>");
		System.err.println(" --port <port>      accept connections on port <port> (default: 4210)");
	}
	
	public static void main(String[] args) {
		int port = 4210;
		String filename = null;
		
		if( args.length == 0 ) {
			usage();
			System.exit(1);
		}
		
		// Parse command line
		for( int i = 0; i < args.length; i++ ) {
			if( args[i].equals("-p") || args[i].equals("--port") ) {
				if( i >= args.length - 1 ) {
					usage();
					System.exit(1);
				}
				port = Integer.parseInt(args[++i]);
			}
			else
				filename = args[i];
		}
		
		
		
		
		Main o = new Main(new BilexicalEvaluator(filename));
		o.run(port);
	}
	
	public Main(Evaluator<SxdgReflection> eval) {
		this.eval = eval;
	}
	
	protected Message processMessage(Message m) {
		if( m.getArgument("parentid") != null )
			parent = m.getArgument("parentid");
		else
			parent = null;
		
		Message sup = super.processMessage(m);
		String type = m.getType();
		
		if( sup == null ) {
			if( type.equals("chooseGlobal") ) {
				try {
					String nextState = space.chooseGlobal();
					
					Map<String,String> args = new HashMap<String,String>();
					args.put("id", nextState);
					
					return new Message("next", args);
				} catch(NoSuchElementException e) {
					return new Message("noUnexpandedState", null);
				}
			}
			
			else
				return rejectMessage();
		}
		else
			return sup;
	}
	
	protected SortedEvaluatingSearchSpace<SxdgReflection> generateNewSpace() {
		return new SortedEvaluatingSearchSpace<SxdgReflection>(eval);
	}
	
	protected SxdgReflection xmlToDomain(String elt) {
		if( parent != null ) {
			return new SxdgReflection(elt, space, space.getStateForName(parent));
		} else {
			return new SxdgReflection(elt, space);
		}
	}
}


