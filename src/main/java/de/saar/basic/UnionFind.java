package de.saar.basic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of the textbook algorithm for the union-find problem. This
 * implementation (based on the union-by-size and path compression heuristics)
 * computes a sequence of union and find operations in nearly linear amortized runtime.
 *
 * @author Alexander Koller
 *
 * @param <E> the datatype of the elements
 */
public class UnionFind<E> {
    private final Map<E,E> tree;             // maps each node to its parent (and roots to themselves)
    private final Map<E,Integer> treesizes;  // maps each root to the number of nodes in its tree

    public UnionFind(Collection<E> baseSet) {
        tree = new HashMap<E,E>();
        treesizes = new HashMap<E,Integer>();

        for( E x : baseSet ) {
            tree.put(x, x);
            treesizes.put(x, 1);
        }
    }

    /**
     * Merges the equivalence classes of two elements, x and y.
     *
     * @param x
     * @param y
     */
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

    /**
     * Returns the representative element of the equivalence class of element x.
     *
     * @param x
     * @return
     */
    public E find(E x) {
        while( ! tree.get(x).equals(x) ) {
            x = tree.get(x);
        }

        return x;
    }
}
