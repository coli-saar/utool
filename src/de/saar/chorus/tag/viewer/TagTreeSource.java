/*
 * @(#)TagTreeSource.java created 25.08.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.tag.viewer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.saar.chorus.XTAGLexicon.Tree;
import de.saar.chorus.jgraph.IGraphSource;
import de.saar.chorus.jgraph.ImprovedJGraph;

public class TagTreeSource implements IGraphSource {
    private List<Tree> trees;
    private Map<Tree,JTagTree> map;
    
    public TagTreeSource(List<Tree> trees) {
        this.trees = trees;
        map = new HashMap<Tree,JTagTree>();
    }
    

    public int size() {
        return trees.size();
    }

    public ImprovedJGraph get(int index) {
        Tree tree = trees.get(index);
        
        if( map.containsKey(tree)) {
            return map.get(tree);
        } else {
            return new JTagTree(tree);
        }
    }

}
