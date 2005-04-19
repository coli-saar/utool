import java.util.*;

public class XDGEntry {

    public List<String> outId;
    public Set<String> inLp;
    public List<String> outLp;
    public String anchor;
    public String anchorAddress;
    public Map<String, Set<String>> linking;
    public String rootCat;
    public int number;
    public String auxDirection = "";
    public boolean isAux = false;

    public XDGEntry(int num){
	outId = new ArrayList<String>();
	inLp = new HashSet<String>();
	outLp = new ArrayList<String>();
	linking = new HashMap<String, Set<String>>();
	number = num;
    }

}
