/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class Annotator {
    private String startAnnotation;
    private String neutralAnnotation;
    private Map<String, Map<String, List<String>>> annotationRules; // annotation -> nodelabel -> childannotations
    private Map<String, Map<String, SetMultimap<Integer,String>>> possibleParentAnnotations; // childannotation -> parentlabel -> childposition -> list(annotation for parent)
    private Set<String> allAnnotations;

    public Annotator() {
        annotationRules = new HashMap<String, Map<String, List<String>>>();
        possibleParentAnnotations = new HashMap<String, Map<String, SetMultimap<Integer, String>>>();
        allAnnotations = new HashSet<String>();
    }

    public String getNeutralAnnotation() {
        return neutralAnnotation;
    }

    public void setNeutralAnnotation(String neutralAnnotation) {
        this.neutralAnnotation = neutralAnnotation;
        allAnnotations.add(neutralAnnotation);
    }

    public String getStartAnnotation() {
        return startAnnotation;
    }

    public void setStartAnnotation(String startAnnotation) {
        this.startAnnotation = startAnnotation;
        allAnnotations.add(startAnnotation);
    }

    public void addRule(String parentAnn, String label, List<String> childAnnotations) {
        Map<String, List<String>> rulesForParent = annotationRules.get(parentAnn);

        if( rulesForParent == null ) {
            rulesForParent = new HashMap<String, List<String>>();
            annotationRules.put(parentAnn, rulesForParent);
        }

        rulesForParent.put(label, childAnnotations);

        for( int i = 0; i < childAnnotations.size(); i++ ) {
            Map<String, SetMultimap<Integer,String>> ppa1 = possibleParentAnnotations.get(childAnnotations.get(i));
            if( ppa1 == null ) {
                ppa1 = new HashMap<String, SetMultimap<Integer, String>>();
                possibleParentAnnotations.put(childAnnotations.get(0), ppa1);
            }

            SetMultimap<Integer,String> ppa2 = ppa1.get(label);
            if( ppa2 == null ) {
                ppa2 = HashMultimap.create();
                ppa1.put(label, ppa2);
            }

            ppa2.put(i, parentAnn);
            allAnnotations.add(parentAnn);
            allAnnotations.addAll(childAnnotations);
        }
    }

    public Set<String> getParentAnnotations(String childAnn, String parentLabel, int childPosition) {
        if( possibleParentAnnotations.containsKey(childAnn)) {
            if( possibleParentAnnotations.get(childAnn).containsKey(parentLabel) ) {
                return possibleParentAnnotations.get(childAnn).get(parentLabel).get(childPosition);
            }
        }

        return null;
    }

    public Set<String> getAllAnnotations() {
        return allAnnotations;
    }

    public List<String> getChildAnnotations(String parentAnnotation, String parentLabel) {
        if( annotationRules.containsKey(parentAnnotation)) {
            return annotationRules.get(parentAnnotation).get(parentLabel);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return startAnnotation + "/" + neutralAnnotation + "/" + annotationRules;
    }


}
