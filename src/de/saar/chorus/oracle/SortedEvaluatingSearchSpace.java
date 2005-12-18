
package de.saar.chorus.oracle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class SortedEvaluatingSearchSpace<DomainType> 
    extends EvaluatingSearchSpace<DomainType>  {

    private ArrayList<String> queue; // queue of state names
    private EvalComparator comp;

    public SortedEvaluatingSearchSpace(Evaluator<DomainType> eval) {
	super(eval);

	comp = new EvalComparator(this);
	queue = new ArrayList<String>();
    }

    public void addState(String id, DomainType contents, String parentId) {
        super.addState(id, contents, parentId);
        
        int insertPos = Collections.binarySearch(queue, id, comp);
        if( insertPos < 0 ) {
            insertPos = -insertPos-1;
        }

        queue.add(insertPos, id);
    }

    public String chooseGlobal() {
        return queue.remove(0);
    }

    public boolean hasUnseenStates() {
	return !queue.isEmpty();
    }
            



    private class EvalComparator implements Comparator<String> {
        private SortedEvaluatingSearchSpace<DomainType> space;

        public EvalComparator(SortedEvaluatingSearchSpace<DomainType> space) {
            this.space = space;
        }

        public int compare(String s1, String s2) {
            double p1 = eval.evaluate(space.getStateForName(s1));
            double p2 = eval.evaluate(space.getStateForName(s2));

            if( p1 < p2 )
                return -1;
            else if( p1 == p2 )
                return 0;
            else
                return 1;
        }
    }
}
