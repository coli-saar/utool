/*
 * @(#)TagTreeSource.java created 25.08.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.tag.viewer;

import java.util.List;

import de.saar.chorus.XTAGLexicon.Tree;
import de.saar.chorus.jgraph.improvedjgraph.LazyGraphSource;

public class TagTreeSource extends LazyGraphSource<JTagTree> {
    private List<Tree> trees;
    
    public TagTreeSource(List<Tree> trees) {
        super(trees.size());
        this.trees = trees;
    }
    
    protected JTagTree compute(int i) {
        return new JTagTree(trees.get(i));
    }
}
