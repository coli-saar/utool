
import java.util.List;
import java.util.Set;

public class SyntInfo {
    
    // Der Index-Wert, zu dem der Entry-Knoten gehoert
    private String index;
    // Die Werte im Anchor-Knoten
    private List<Anchor> anchors;
    // Die Werte im Trees-Knoten
    private Set<String> trees;
    // Die Werte im Families-Knoten
    private Set<String> families;

    public SyntInfo(String index, 
		    List<Anchor> anchors,
		    Set<String> trees,
		    Set<String> families)
    {
	this.index = index;
	this.anchors = anchors;
	this.trees = trees;
	this.families = families;
    }

    public Set<String> getTrees (){
	return trees;
    }

    public Set<String> getFamilies (){
	return families;
    }

    public List<Anchor> getAnchors (){
	return anchors;
    }


    public int hashCode() {
	return index.hashCode();
    }

}
