/*
 * @(#)NodeType.java created 25.08.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.tag.viewer;

public enum NodeType {
    internal,
    
    anchor {
        public String getMarker() {
            return "<>";
        }
    },
    
    subst {
      public String getMarker() {
          return "!";
      }
    },
    
    foot {
      public String getMarker() {
          return "*";
      }
    },
    
    terminal;

    
    
    public String getMarker() {
        return "";
    }
}
