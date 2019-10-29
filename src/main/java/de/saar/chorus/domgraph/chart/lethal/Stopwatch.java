/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author koller
 */
public class Stopwatch {
    private Map<String,Long> startTimes;
    private boolean printMessages;

    public Stopwatch() {
        this(true);
    }

    public Stopwatch(boolean printMessages) {
        this.printMessages = printMessages;
        startTimes = new HashMap<String, Long>();
    }

    public void start(String key) {
        System.err.print("Computing " + key + " ... ");
        startTimes.put(key, System.currentTimeMillis());
    }

    public void report(String key, String label) {
        long diff = System.currentTimeMillis() - startTimes.get(key);
        System.err.println(label + ", " + diff + " ms.");
    }
}
