
package de.saar.coli.chorus.oracle.dummy;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import de.saar.coli.chorus.oracle.*;


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



public class Main extends EvaluatingOracle<DummySpace,String> {
	private DummyEvaluator eval;
	
	
	public static void main(String[] args) {
		DummyEvaluator eval = new DummyEvaluator();
		
		Main o = new Main(eval);
		o.run(4210);
	}
	
	public Main(DummyEvaluator eval) {
		this.eval = eval;
	}
	
	protected Message processMessage(Message m) {
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
	
	/*
	protected void processReset() {
		space = new DummySpace((DummyEvaluator) eval);
	}
	*/
	
	protected String xmlToDomain(org.w3c.dom.Element elt) {
		if( elt == null )
			return null;
		else
			return elt.getTagName();
	}

	/* (non-Javadoc)
	 * @see de.saar.coli.chorus.oracle.Oracle#generateNewSpace()
	 */
	protected DummySpace generateNewSpace() {
		return new DummySpace(eval);
	}

	/* (non-Javadoc)
	 * @see de.saar.coli.chorus.oracle.Oracle#xmlToDomain(java.lang.String)
	 */
	protected String xmlToDomain(String elt) {
		return elt;
	}
	
}


