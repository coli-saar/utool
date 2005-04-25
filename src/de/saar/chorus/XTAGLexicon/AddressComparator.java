import java.util.*;

public class AddressComparator implements Comparator<String>{

    public int compare (String a1, String a2){
	if (a1.equals(a2)){
	    return 0;}
	else{
	    String[] strA1 = a1.split("\\.");
	    String[] strA2 = a2.split("\\.");
	    for (int i = 0; i < Math.min(strA1.length, strA2.length); i++){
		if (!(strA1[i].equals(strA2[i]))){
		    int intA1 = Integer.getInteger(strA1[i], -3).intValue();
		    int intA2 = Integer.getInteger(strA2[i], -3).intValue();
		    if (intA1 < intA2){
			return -1;}
		    else return 1;}
	    }
	    if (strA1.length < strA2.length){
		return -1;}
	    else {return 1;}
	}
    }

    public boolean equals (Object o){
	if ( o.equals(this) )
	    return true;
	else {return false;}
    }

}
