/*
 * @(#)ChartPresenter.java created 21.04.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.saar.chorus.domgraph.graph.DomGraph;

public class ChartPresenter {
    public static <E extends SubgraphNonterminal> String chartOnlyRoots(RegularTreeGrammar<E> ch, DomGraph g) {
        StringBuffer ret = new StringBuffer();
        Set<String> roots = g.getAllRoots();
        Set<E> visited = new HashSet<E>();

        for( E fragset : ch.getToplevelSubgraphs() ) {
            ret.append(corSubgraph(fragset, ch, roots, visited));
        }

        return ret.toString();
    }



    private static <E extends SubgraphNonterminal> String corSubgraph(E subgraph, RegularTreeGrammar<E> ch, Set<String> roots, Set<E> visited) {
        Set<String> s = new HashSet<String>(subgraph);
        StringBuffer ret = new StringBuffer();
        boolean first = true;
        String whitespace = "                                                                                                                                                                                  ";
        Set<E> toVisit = new HashSet<E>();

        if( !visited.contains(subgraph )) {
            visited.add(subgraph);

            s.retainAll(roots);
            String sgs = s.toString();


            if( ch.getSplitsFor(subgraph) != null ) {
                ret.append("\n" + sgs + " -> ");
                for( Split split : ch.getSplitsFor(subgraph)) {
                    if( first ) {
                        first = false;
                    } else {
                        ret.append(whitespace.substring(0, sgs.length() + 4));
                    }

                    ret.append(corSplit(split, roots) + "\n");
                    toVisit.addAll(split.getAllSubgraphs());
                }

                for( E sub : toVisit ) {
                    ret.append(corSubgraph(sub, ch, roots, visited));
                }
            }

            return ret.toString();
        }
        else {
            return "";
        }
    }



    private static <E extends SubgraphNonterminal> String corSplit(Split<E> split, Set<String> roots) {
        StringBuffer ret = new StringBuffer("<" + split.getRootFragment());
        Map<String,List<Set<String>>> map = new HashMap<String,List<Set<String>>> ();

        for( String hole : split.getAllDominators() ) {
            List<Set<String>> x = new ArrayList<Set<String>>();
            map.put(hole, x);

            for( E wcc : split.getWccs(hole) ) {
                Set<String> copy = new HashSet<String>(wcc);
                copy.retainAll(roots);
                x.add(copy);
            }
        }


        ret.append(" " + map);
        ret.append(">");
        return ret.toString();
    }
}
