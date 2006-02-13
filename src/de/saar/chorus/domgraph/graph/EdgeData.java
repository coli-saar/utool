/*
 * @(#)EdgeData.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.graph;

public class EdgeData {

    private EdgeType type;
        

    /**
     * @param type
     * @param name
     */
    public EdgeData(EdgeType type) {
        this.type = type;
    }


    /**
     * @return Returns the type.
     */
    public EdgeType getType() {
        return type;
    }
    
    public String getDesc() {
        return "(edge type=" + type + ")";
    }
    
    public String toString() {
        return "[E:" + type + "]";
    }
}
