package de.saar.basic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UnionFind<E> {
    private final Map<E,E> tree;
    private final Map<E,Integer> treesizes;

    public UnionFind(Collection<E> baseSet) {
        tree = new HashMap<E,E>();
        treesizes = new HashMap<E,Integer>();

        for( E x : baseSet ) {
            tree.put(x, x);
            treesizes.put(x, 1);
        }
    }

    public void union(E x, E y) {
        E xroot = find(x);
        E yroot = find(y);

        // attach the smaller tree below the larger tree
        if( treesizes.get(xroot) > treesizes.get(yroot) ) {
            tree.put(yroot, xroot);
            pathCompression(y,xroot);
            treesizes.put(xroot, treesizes.get(xroot) + treesizes.get(yroot));
        } else {
            tree.put(xroot, yroot);
            pathCompression(x,yroot);
            treesizes.put(yroot, treesizes.get(xroot) + treesizes.get(yroot));
        }
    }

    private void pathCompression(E x, E newroot) {
        while( ! tree.get(x).equals(newroot) ) {
            E oldparent = tree.get(x);
            tree.put(x, newroot);
            x = oldparent;
        }
    }

    public E find(E x) {
        if( tree.get(x).equals(x) ) {
            return x;
        } else {
            return find(tree.get(x));
        }
    }
}
