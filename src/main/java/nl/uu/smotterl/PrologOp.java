
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
 *   PrologOp.makeops();
 *   prologtokenizer tok = new prologtokenizer(some_term);
 *   term t = tok.gettermdot(null);
 */


import java.util.Hashtable;




/**********************************************************
 *  P R O L O G O P
 **********************************************************/

public class PrologOp {
    boolean prex,postx;
    int place,priority;
    static int pre=1,in=2,post=3;
    String name;
    static String AND,OR,MATCH,ARROW,CUT,REWRITE;
    /*all operators in play are defined here:*/
    static Hashtable preops,inops,postops;
    static PrologOp listcons;

    PrologOp(){}/*empty constructor.
                  use make as a (sometimes failing) constructor*/

    static PrologOp make(String n,String type,int prior) {
        /*returns such an operator, or null*/
        if(prior<0||prior>1200)
            return null;
        PrologOp p=new PrologOp();
        p.name=n;
        p.priority=prior;
        if(type.length()==2&&type.charAt(0)=='f')
            {p.place=pre;
            if(type.equals("fx"))
                p.postx=true;
            else if(type.equals("fy"))
                p.postx=false;
            else
                return null;
            return p;
            }
        else if(type.length()==2&&type.charAt(1)=='f')
            {p.place=post;
            if(type.equals("xf"))
                p.prex=true;
            else if(type.equals("fy"))
                p.prex=false;
            else
                return null;
            return p;
            }
        else if(type.length()==3&&type.charAt(1)=='f') {
            p.place=in;
            if(type.equals("xfx")) {
                p.prex=true;
                p.postx=true;
            }
            else if(type.equals("xfy")) {
                p.prex=true;
                p.postx=false;
            }
            else if(type.equals("yfx")) {
                p.prex=false;
                p.postx=true;
            }/*note that yfy would give rise to ambiguity*/
            else return null;
            return p;
        }
        return null;
    }

    public static void makeops() {
        if(Term.emptylist!=null)
            return;
        Term.emptylist=Term.newconstant("[]","[]");

        AND=",";
        OR=";";
        MATCH="=";
        ARROW=":-";
        CUT="!";
        REWRITE="-->";

        preops=new Hashtable();
        inops=new Hashtable();
        postops=new Hashtable();
        addoperator("?-","fx",1200);//
        addoperator(ARROW,"xfx",1200);//the if
        addoperator(ARROW,"fx",1200);//the do in programs
        addoperator(REWRITE,"xfx",1200);//grammar rules
        addoperator("not","fx",900);
        addoperator(OR,"xfy",1100);//the ;
        addoperator(AND,"xfy",1000);//the ,
        addoperator(MATCH,"xfx",700);//matchable
        addoperator("==","xfx",700);//exactly the same
        addoperator("\\==","xfx",700);//not the same
        addoperator(">","xfx",700);//compare values
        addoperator("<","xfx",700);//compare values
        addoperator(">=","xfx",700);//compare values
        addoperator("<=","xfx",700);//compare values
        addoperator("is","xfx",700);// calculate right
        addoperator("=:=","xfx",700);//values equal
        addoperator("=\\=","xfx",700);//values unequal
        addoperator("=..","xfx",700);//compose a(b)=..[a,b]

        addoperator("+","yfx",500);
        addoperator("-","yfx",500);
        addoperator("-","fx",500);
        addoperator("+","fx",500);
        addoperator("*","yfx",400);
        addoperator("/","yfx",400);
        addoperator("div","yfx",400);
        addoperator("mod","xfx",300);

        listcons=make(".","xfy",600);
    }
    public static boolean addoperator(String s,String type,int level) {
        PrologOp op=make(s,type,level);
        if(op==null)
            return false;
        if(op.place==op.pre)
            preops.put(s,op);
        else if(op.place==op.in)
            inops.put(s,op);
        else
            postops.put(s,op);
        return true;
    }

    public static PrologOp preop(String name)
    {return (PrologOp)preops.get(name);}
    public static PrologOp inop(String name)
    {return (PrologOp)inops.get(name);}
    public static PrologOp postop(String name)
    {return (PrologOp)postops.get(name);}

    int under(PrologOp o1,PrologOp o2) {
        /*1 means that that o1 can be under o2: like 3*4+2
          2 means 2+3*4.
          0 means they cannot be combined. for example let <-- be xfx,
          then a <-- b <-- c is a syntax error.
        */
        if(o1.priority<o2.priority)
            return 1;
        if(o1.priority>o2.priority)
            return 2;
        if(!o2.prex)
            return 1;
        if(!o1.postx)
            return 2;
        return 0;
    }
    int leftunderlevel() {
        if(prex)
            return priority-1;
        return priority;
    }
    int rightunderlevel() {
        if(postx)
            return priority-1;
        return priority;
    }

}

