
package de.saar.theoremprovers.mace;

import java.util.*;



class ModelTable {
    private int arity;
    private int[] table;
    private int modelsize;
    private String label;

    
    ModelTable(String label, int arity, int[] table) {
	this.label = label;
	this.arity = arity;
	this.table = table;

	modelsize = computeModelsize();
    }

    private int pow(int base, int exp) {
	int accu = 1;

	for( int i = 0; i < exp; i++ )
	    accu *= base;

	return accu;
    }

    private int computeModelsize() {
	for( int i = 1; i < table.length; i++ )
	    if( pow(i, arity)  == table.length )
		return i;

	return -1;
    }

    public int getArity() {
	return arity;
    }

    public int[] getTable() {
	return table;
    }

    public String getLabel() {
	return label;
    }

    public int getTableEntry(int row, int col) {
	return table[col + modelsize*row];
    }

    public String toString() {
	String ret = "<" + label + "/" + arity + ":";
	for( int i = 0; i < table.length; i++ )
	    ret += " " + table[i];
	return ret + ">";
    }
}
