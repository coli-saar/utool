
package de.saar.chorus.oracle;

import java.util.HashMap;
import java.util.Map;

public abstract class EvaluatingOracle<SpaceType extends EvaluatingSearchSpace<DomainType>,
DomainType>
extends Oracle<SpaceType,DomainType> {
	
	protected Message processMessage(Message m) {
		Message sup = super.processMessage(m);
		String type = m.getType();
		
		if( sup == null ) {
			if( type.equals("evaluate") ) {
				String id = m.getArgument("id");
				double evaluated = space.evaluate(id);
				
				Map<String,String> map = new HashMap<String,String>();
				map.put("value", new Double(evaluated).toString());
				
				return new Message("evaluated", map);
				
			} else if( type.equals("minLocal") ) {
				String parentid = m.getArgument("parentid");
				StateEvaluation evaluated = space.minLocal(parentid);
				
				Map<String,String> map = new HashMap<String,String>();
				map.put("value", new Double(evaluated.getEval()).toString());
				map.put("childid", evaluated.getStateName());
				
				return new Message("minLocalResult", map);
				
			} else if( type.equals("minGlobal") ) {
				StateEvaluation evaluated = space.minGlobal();
				
				Map<String,String> map = new HashMap<String,String>();
				map.put("value", new Double(evaluated.getEval()).toString());
				map.put("id", evaluated.getStateName());
				
				return new Message("minGlobalResult", map);
			}       
			
			else
				return null;
		} else
			return sup;
	}
}
