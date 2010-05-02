/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

/**
 *
 * @author koller
 */
public class RewriteSystem {
    private SetMultimap<String,String> rules;

    public RewriteSystem() {
        rules = HashMultimap.create();
    }

    public void addRule(String f1, int n1, String f2, int n2, String annotation) {
        rules.put(key(f1,n1,f2,n2), annotation);
    }


    private String key(String f1, int n1, String f2, int n2) {
        return f1 + "_" + n1 + "_" + f2 + "_" + n2;
    }

    @Override
    public String toString() {
        return rules.toString();
    }


}
