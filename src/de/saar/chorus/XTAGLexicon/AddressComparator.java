import java.util.*;

public class AddressComparator implements Comparator<String>{

    public int compare (String a1, String a2){
	try {
	String[] strA1 = a1.split("\\.");
	String[] strA2 = a2.split("\\.");
	for (int i = 2; i < Math.min(strA1.length, strA2.length); i++){
	    if (!(strA1[i].equals(strA2[i]))){
		int intA1 = Integer.parseInt(strA1[i]);
		int intA2 = Integer.parseInt(strA2[i]);

		//Integer intA1 = Integer.getInteger(strA1[i]);
		//Integer intA2 = Integer.getInteger(strA2[i]);
		
		//return intA1.compareTo(intA2);

		// Achtung: funktioniert das auch für 10, 11, 12, ...?
		/***return strA1[i].compareTo(strA2[i]); */

		if (intA1 < intA2){
		    return -1;}
		else return 1; 
	    }
	}
	if (strA1.length < strA2.length){
	    /*** A1 ist Vorgaenger von A2: L.A1 vor allen Addressen
		 von A2, R.A1 nach allen Addressen */
	    return -1;}
	else {if (strA1.length == strA2.length){
	    /*** L < M < R */
	    return 0;}
	else return 1;}
	} catch (Exception e) {
	    System.err.println("["+ a1 + "," + a2 + "]");
	}
	return 0;
    }

    public boolean equals (Object o){
	if ( o.equals(this) )
	    return true;
	else {return false;}
    }

}
