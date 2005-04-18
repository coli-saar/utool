import java.util.*;


public class XDGWriter {

    public printHeader (StringBuffer sb, Converter con){
	sb.append(
"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n
<!DOCTYPE grammar SYSTEM \"../Compiler/XML/xdk.dtd\">\n
<grammar>\n
    <useDimension idref=\"id\"/>\n
    <useDimension idref=\"lp\"/>\n
    <useDimension idref=\"idlp\"/>\n
    <useDimension idref=\"lex\"/>\n
    <dimension id=\"id\">\n
	<attrsType>\n
	    <typeRecord>\n
	    </typeRecord>\n
	</attrsType>\n
	<entryType>\n
	    <typeRecord>\n
		<typeFeature data=\"in\">\n
		    <typeISet>\n
			<typeRef idref=\"id.label\"/>\n
		    </typeISet>\n
		</typeFeature>\n
		<typeFeature data=\"out\">\n
		    <typeValency>\n
			<typeRef idref=\"id.label\"/>\n
		    </typeValency>\n
		</typeFeature>\n
	    </typeRecord>\n
	</entryType>\n
	<labelType>\n
	    <typeRef idref=\"id.label\"/>\n
	</labelType>\n
	<typeDef id=\"id.label\">\n
	    <typeDomain>\n");
	//<!-- HIER: die Labels fuer \"ID\", z.B. S_A_, NP_, ... -->\n
	//<constant data=\"lS\"/>\n");
	sb.append(
"	     </typeDomain>\n
	</typeDef>\n
	<usePrinciple idref=\"principle.graph1\">\n
	    <dim var=\"D\" idref=\"id\"/>\n\n
	</usePrinciple>\n
	<usePrinciple idref=\"principle.tree\">\n
	    <dim var=\"D\" idref=\"id\"/>\n\n
	</usePrinciple>\n
	<usePrinciple idref=\"principle.in\">\n
	    <dim var=\"D\n\" idref=\"id\"/>\n
	    <arg var=\"In\">\n
		<featurePath root=\"down\" dimension=\"D\" aspect=\"entry\">\n
		    <constant data=\"in\"/>\n
		</featurePath>\n
	    </arg>\n
	</usePrinciple>\n
	<usePrinciple idref=\"principle.out\">\n
	    <dim var=\"D\" idref=\"id\"/>\n
	    <arg var=\"Out\">\n
		<featurePath root=\"down\" dimension=\"D\" aspect=\"entry\">\n
		    <constant data=\"out\"/>\n
		</featurePath>\n
	    </arg>\n
	</usePrinciple>\n
	<output idref=\"output.pretty\"/>\n\n
    </dimension>\n
    <dimension id=\"lp\">\n
	<attrsType>\n
	    <typeRecord>\n\n
	    </typeRecord>\n\n
	</attrsType>\n
	<entryType>\n
	    <typeRecord>\n
		<typeFeature data=\"in\">\n
		    <typeISet>\n
			<typeRef idref=\"lp.label\"/>\n
		    </typeISet>\n
		</typeFeature>\n
		<typeFeature data=\"out\">\n
		    <typeValency>\n
			<typeRef idref=\"lp.label\"/>\n
		    </typeValency>\n
		</typeFeature>\n
		<typeFeature data=\"on\">\n
		    <typeISet>\n
			<typeRef idref=\"lp.label\"/>\n
		    </typeISet>\n
		</typeFeature>\n
	    </typeRecord>\n
	</entryType>\n
	<labelType>\n
	    <typeRef idref=\"lp.label\"/>\n
	</labelType>\
	<typeDef id=\"lp.label\">\n
	    <typeDomain>\n");
	//<!-- HIER: alle Knotenadressen (+ ggf. _L/_R) -->\n
	//<constant data=\"p0l\"/>\n");
	sb.append(
"	    </typeDomain>\n
	</typeDef>\n
	<usePrinciple idref=\"principle.graph1\">\n
	    <dim var=\"D\" idref=\"lp\"/>\n\n
	</usePrinciple>\n
	<usePrinciple idref=\"principle.tree\">\n
	    <dim var=\"D\" idref=\"lp\"/>\n\n
	</usePrinciple>\n
	<usePrinciple idref=\"principle.in\">\n
	    <dim var=\"D\" idref=\"lp\"/>\n
	    <arg var=\"In\">\n
		<featurePath root=\"down\" dimension=\"D\" aspect=\"entry\">\n
		    <constant data=\"in\"/>\n
		</featurePath>\n
	    </arg>\n
	</usePrinciple>\n
	<usePrinciple idref=\"principle.out\">\n
	    <dim var=\"D\" idref=\"lp\"/>\n
	    <arg var=\"Out\">\n
		<featurePath root=\"down\" dimension=\"D\" aspect=\"entry\">\n
		    <constant data=\"out\"/>\n
		</featurePath>\n
	    </arg>\n
	</usePrinciple>\n
	<usePrinciple idref=\"principle.order\">\n
	    <dim var=\"D\" idref=\"lp\"/>\n
	    <arg var=\"On\">\n
		<featurePath root=\"down\" dimension=\"D\" aspect=\"entry\">\n
		    <constant data=\"on\"/>\n
		</featurePath>\n
	    </arg>\n
	    <arg var=\"Order\">\n
		<list>\n");
	//<!-- HIER: die Reihenfolge der LP-Label -->\n
	//    <constant data=\"p0l\"/>\n");
	sb.append(
"		 </list>\n
	    </arg>\n
	    <arg var=\"Projective\">\n
		<constant data=\"true\"/>\n
	    </arg>\n
	    <arg var=\"Yields\">\n
		<constant data=\"true\"/>\n
	    </arg>\n
	</usePrinciple>\n
	<usePrinciple idref=\"principle.parse\">\n
	    <dim var=\"D\" idref=\"lp\"/>\n\n
	</usePrinciple>\n
	<output idref=\"output.pretty\"/>\n\n
    </dimension>\n
    <dimension id=\"idlp\">\n
	<attrsType>\n
	    <typeRecord>\n\n
	    </typeRecord>\n
	</attrsType>\n
	<entryType>\n
	    <typeRecord>\n
		<typeFeature data=\"link\">\n
		    <typeMap>\n
			<typeRef idref=\"lp.label\"/>\n
			<typeSet>\n
			    <typeRef idref=\"id.label\"/>\n
			</typeSet>\n
		    </typeMap>\n
		</typeFeature>\n
	    </typeRecord>\n
	</entryType>\n
	<labelType>\n
	    <typeDomain>\n\n
	    </typeDomain>\n
	</labelType>\n\n
	<usePrinciple idref=\"principle.linkingDaughterEnd\">\n
	    <dim var=\"D1\" idref=\"lp\"/>\n
	    <dim var=\"D2\" idref=\"id\"/>\n
	    <arg var=\"End\">\n
		<featurePath root=\"up\" dimension=\"This\" aspect=\"entry\">\n
		    <constant data=\"link\"/>\n
		</featurePath>\n
	    </arg>\n
	</usePrinciple>\n
	<output idref=\"output.pretty\"/>\n\n
    </dimension>\n
    <dimension id=\"lex\">\n
	<attrsType>\n
	    <typeRecord>\n\n
	    </typeRecord>\n
	</attrsType>\n
	<entryType>\n
	    <typeRecord>\n
		<typeFeature data=\"word\">\n
		    <typeString/>\n
		</typeFeature>\n
	    </typeRecord>\n
	</entryType>\n
	<labelType>\n
	    <typeDomain>\n\n
            </typeDomain>\n
	</labelType>\n\n\n
	<output idref=\"output.dags1\"/>\n
	<output idref=\"output.latexs1\"/>\n
	<useOutput idref=\"output.dags1\"/>\n
    </dimension>\n");
}

    public printEntry (StringBuffer sb, XDGEntry entry){
	sb.append(
"     <entry>\n
	<classConj>\n
	    <classDimension idref=\"id\">\n
		<record>\n
		    <feature data=\"in\">\n
			<set>\n");
	//<!-- HIER -->\n
	//	    <constant data=\"lS\"/>\n");
	sb.append(
"			 </set>\n
		    </feature>\n
		    <feature data="out">\n
			<set>\n");
	//<!-- HIER -->\n
	//                  <!-- genau 2 NPs -->\n
	//	    <constantCardSet data=\"lNP\">\n
	//		<integer data=\"2\"/>\n
	//	    </constantCardSet>\n
	//                  <!-- bis zu zwei NPs:\n
	//                  <constantCardSet data=\"NP_A_\">\n
	//		<integer data=\"0\"/>\n
	//		<integer data=\"1\"/>\n
	//		<integer data=\"2\"/>\n
	//	    </constantCardSet>\n
	//                   -->\n
	//	    <constantCard data=\"NP_!\" card=\"one\"/>\n
	//	    <constantCard data=\"lS\" card=\"opt\"/>\n");
	sb.append(
"			 </set>\n
		    </feature>\n
		</record>\n
	    </classDimension>\n
	    <classDimension idref=\"lp\">\n
		<record>\n
		    <feature data=\"in\">\n");
	//			<top/>\n
	sb.append(		
"		   </feature>\n
		    <feature data=\"out\">\n
			<set>\n");
	//<!-- HIER -->
	//	    <constantCard data="p0l" card="opt"/>
	//	    <constantCard data="p0r" card="opt"/>
	//	    <constantCard data="p1l" card="one"/>
	//	    <constantCard data="p22l" card="one"/>
	sb.append(
"			 </set>\n
		    </feature>\n
		    <feature data=\"on\">\n
			<set>\n");
	//<!-- HIER: die Ankeradresse -->
	//	    <constant data="p211l"/>
		sb.append(
"       	         </set>\n
		    </feature>\n
		</record>\n
	    </classDimension>\n
	    <classDimension idref=\"idlp\">\n
		<record>\n
		    <feature data="link">\n
			<record>\n");
		//<!-- HIER -->
		//    <feature data="p0l">
		//	<set>
		//	    <constant data="lS"/>
		//	</set>
		//    </feature>
		//    <feature data="p0r">
		//	<set>
		//	    <constant data="lS"/>
		//	</set>
		//    </feature>
		sb.append(
"			 </record>\n
		    </feature>\n
		</record>\n
	    </classDimension>\n
	    <classDimension idref=\"lex\">\n
		<record>\n
		    <feature data=\"word\">\n");
		//                        <!-- HIER -->
		//<constant data="loves"/>
		sb.append(
" 		     </feature>\n
		</record>\n
	    </classDimension>\n
	</classConj>\n
    </entry>\n");
    }

}
