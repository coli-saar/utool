
package nl.uu.smotterl;

/**
 * CKI Prolog is an prolog interpreter
 * Sieuwert van Otterloo
 * http://www.students.cs.uu.nl/~smotterl/prolog
 * smotterl@cs.uu.nl
 * 1999
 */

/**
 * Note: from on the web site
 * http://www.students.cs.uu.nl/~smotterl/prolog
 * is a message:
 * "The sourcecode of CKI prolog is free. This means
 * that it can be modified by anyone."
 *
 * In addition, I received specific permission from
 * Sieuwert van Otterloo use this code for an example
 * for my Java AI book.  -Mark Watson
 */

/**
 * Note #2: Sieuwert's original code was a nice
 * Java applet with a user interface for Prolog.
 * I removed the user interface code and added an
 * API (top level Prolog class) for using the
 * Prolog engine in Java applications. -Mark Watson
 */

/**
 * Note #3: I only took over the classes related to Prolog 
 * _parsing_. The original program also contains stuff to
 * actually _interpret_ Prolog programs. - Alexander Koller
 *
 * Usage: 
 *   prologop.makeops();
 *   prologtokenizer tok = new prologtokenizer(some_term);
 *   term t = tok.gettermdot(null);
 */


import java.util.Vector;



/*The prologtokenizer breaks a string in such pieces that are usefull
for parsing. It searches for constants, numbers, or special operators,
and else it returns the characters one by one.
It ignores spaces, tabs,enters and %comment.*/

public class PrologTokenizer {
    String unused;/*the unprocessed part of the string*/
    Vector tokens;/*the allready extracted tokens*/
    int cursor;/*the current position in the token vector.*/

    static String normalchar=
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz1234567890";
    static String opchar="+-*/\\^<>=`~:.?@#$&";
    static String numchar="1234567890";

    boolean splitoff() {
        /*extract a token from unused, and adds it to tokens*/
        /*step 1: skip whitespace
          2: decide on first character what the token kind is.
          3: seek the end of the token (start)
          4: shorten unused, add the token, return true;*/
        if(unused==null)            return false;
        int max=unused.length();
        int start=0;
        boolean comment=false;
        for(start=0;start<max;start++) {
            if(unused.charAt(start)=='%')
                comment=true;
            else if(unused.charAt(start)=='\n')
                comment=false;
            if(!comment&&unused.charAt(start)>32)
                break;
        }
        if(start==max) {
            unused=null;return false;
        }
        StringBuffer buf=new StringBuffer();
        char d;
        char c=unused.charAt(start);
        start++;
        buf.append(c);
        if(c==39||c=='"') {
            boolean closed=false;
            for(;start<max;start++) {
                d=unused.charAt(start);
                if(d==c) {
                    start++;
                    if(start<max&&unused.charAt(start)==c);//mind the ;
                    else
                        {closed=true;break;}
                }
                buf.append(d);
            }
            if(!closed)
                return false;
        }
        else
            if(c=='\"') {
                boolean closed=false;
                for(;start<max;start++) {
                    d=unused.charAt(start);
                    if(d==c) {
                        start++;
                        if(start<max&&unused.charAt(start)==c);//mind the ;
                        else
                            {buf.append(d);closed=true;break;}
                    }
                    buf.append(d);
                }
            }
            else if(in(c,numchar)) { //number
                for(;start<max;start++) {
                    d=unused.charAt(start);
                    if(!in(unused.charAt(start),numchar))
                        break;
                    buf.append(d);
                }
            }  else if(in(c,opchar)) { //a special operator
                for(;start<max;start++) {
                    d=unused.charAt(start);
                    if(!in(unused.charAt(start),opchar))
                        break;
                    buf.append(d);
                }
            } else if(in(c,normalchar)) {//normal constant
                for(;start<max;start++) {
                    d=unused.charAt(start);
                    if(!in(unused.charAt(start),normalchar))
                        break;
                    buf.append(d);
                }
            }

        tokens.addElement(buf.toString());
        unused=unused.substring(start);
        return true;
    }

    public PrologTokenizer(String s) {
        unused=s;
        cursor=0;
        tokens=new Vector();
    }

    public Term gettermdot(Thread t) {
        /*get a term, closed by a . (dot or period).*/
        Term t1=Term.getTerm(this);
        if(t1!=null&&".".equals(gettoken()))
            return t1;
        return null;
    }

    char get0() {
        /*get a single character.*/
        char c='*';
        if(unused!=null&&unused.length()>0) {
            c=unused.charAt(0);
            unused=unused.substring(1);
        }
        return c;
    }

    public boolean more() {
        /*do we have more tokens?*/
        if(cursor<tokens.size())
            return true;
        return splitoff();
    }
    public String peek() {
        /*returns the first token, but does not remove it.*/
        if(cursor>tokens.size())
            return null;
        if(cursor==tokens.size())
            if(!splitoff())return null;
        return (String)tokens.elementAt(cursor);
    }
    public String gettoken() {
        /*removes a token out of this tokenizer, and returns that token*/
        if(!more())
            return null;
        cursor++;
        return (String)tokens.elementAt(cursor-1);
    }
    int getpos()/*return the position in the tokenvector*/
    {return cursor;}
    void jumpto(int i)/*jump to a position in the tokenvector*/
    {cursor=i;}

    static boolean in(char c,String s) {
        /*tells wether a char is in a string.*/
        for(int i=s.length()-1;i>=0;i--)
            if(c==s.charAt(i))
                return true;
        return false;
    }
}

