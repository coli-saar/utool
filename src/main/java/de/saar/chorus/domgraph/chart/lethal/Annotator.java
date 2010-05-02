/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author koller
 */
public class Annotator {
    private String startAnnotation;
    private String neutralAnnotation;
    private Map<String, Map<String, List<String>>> annotationRules; // annotation -> nodelabel -> childannotations

    public Annotator() {
        annotationRules = new HashMap<String, Map<String, List<String>>>();
    }

    public String getNeutralAnnotation() {
        return neutralAnnotation;
    }

    public void setNeutralAnnotation(String neutralAnnotation) {
        this.neutralAnnotation = neutralAnnotation;
    }

    public String getStartAnnotation() {
        return startAnnotation;
    }

    public void setStartAnnotation(String startAnnotation) {
        this.startAnnotation = startAnnotation;
    }

    public void addRule(String parentAnn, String label, List<String> childAnnotations) {
        Map<String, List<String>> rulesForParent = annotationRules.get(parentAnn);

        if( rulesForParent == null ) {
            rulesForParent = new HashMap<String, List<String>>();
            annotationRules.put(parentAnn, rulesForParent);
        }

        rulesForParent.put(label, childAnnotations);
    }

    @Override
    public String toString() {
        return startAnnotation + "/" + neutralAnnotation + "/" + annotationRules;
    }


}
