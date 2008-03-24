package de.saar.chorus.domgraph.chart.rtgparser;


public class StringNonterminal  {
    private final String string;



    public StringNonterminal(String string) {
        super();
        this.string = string;
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
    public int hashCode() {
        return string.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if( obj instanceof StringNonterminal ) {
            StringNonterminal new_name = (StringNonterminal) obj;

            return new_name.string.equals(this.string);

        } else {
            return false;
        }
    }
}
