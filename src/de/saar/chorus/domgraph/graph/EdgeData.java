/*
 * @(#)EdgeData.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.graph;

public class EdgeData {

    private EdgeType type;
    private String name;
        

    /**
     * @param type
     * @param name
     */
    public EdgeData(EdgeType type, String name) {
        this.type = type;
        this.name = name;
    }
    
    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }
    /**
     * @return Returns the type.
     */
    public EdgeType getType() {
        return type;
    }
    
    public String getDesc() {
        return "(edge " + name + " type=" + type + ")";
    }
    
    public String toString() {
        return "[E:" + type + "/" + name + "]";
    }
}
