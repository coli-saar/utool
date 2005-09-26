package de.saar.chorus.poetry;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import de.saar.chorus.XTAGLexicon.Node;
import de.saar.chorus.XTAGLexicon.XDGEntry;

class XdgWriter {
	    public void printHeader (Writer sb, 
	            Collection<String> addresses, 
	            Collection<String> labels) throws IOException {
	        sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"+
	                "<!DOCTYPE grammar SYSTEM \"../Compiler/XML/xdk.dtd\">\n"+
	                "<grammar>\n"+
	                "    <useDimension idref=\"id\"/>\n"+
	                "    <useDimension idref=\"lp\"/>\n"+
	                "    <useDimension idref=\"idlp\"/>\n"+
	                "    <useDimension idref=\"lex\"/>\n"+
	                
	                
	                /************ DIMENSION: id ***************/
	                "    <dimension id=\"id\">\n"+
	                "	 <attrsType>\n"+
	                "            <typeRecord>\n"+
	                "	     </typeRecord>\n"+
	                "	 </attrsType>\n"+
	                "	 <entryType>\n"+
	                "	     <typeRecord>\n"+
	                "		 <typeFeature data=\"in\">\n"+
	                "		     <typeISet>\n"+
	                "			 <typeRef idref=\"id.label\"/>\n"+
	                "		     </typeISet>\n"+
	                "		 </typeFeature>\n"+
	                "		 <typeFeature data=\"out\">\n"+
	                "		     <typeValency>\n"+
	                "			 <typeRef idref=\"id.label\"/>\n"+
	                "		     </typeValency>\n"+
	                "		 </typeFeature>\n"+
	                "	     </typeRecord>\n"+
	                "	 </entryType>\n"+
	                "	 <labelType>\n"+
	                "	     <typeRef idref=\"id.label\"/>\n"+
	                "	 </labelType>\n"+
	                "	 <typeDef id=\"id.label\">\n"+
	        "	     <typeDomain>\n");
	        
	        //sb.append("	      <constant data=\"sentence\"/>\n");
	        sb.append("	      <constant data=\"dum\"/>\n");
	        //<!-- HIER: die Labels fuer \"ID\", z.B. S_A_, NP_, ... -->\n
	        for (String label : labels) {
	            sb.append("	      <constant data=\""+label+"\"/>\n");
	        }
	        
	        sb.append("	     </typeDomain>\n"+
	                "	 </typeDef>\n"+
	                "	 <usePrinciple idref=\"principle.graph1\">\n"+
	                "	     <dim var=\"D\" idref=\"id\"/>\n\n"+
	                "	 </usePrinciple>\n"+
	                "	 <usePrinciple idref=\"principle.tree\">\n"+
	                "	     <dim var=\"D\" idref=\"id\"/>\n\n"+
	                "	 </usePrinciple>\n"+
	                "	 <usePrinciple idref=\"principle.in\">\n"+
	                "	     <dim var=\"D\" idref=\"id\"/>\n"+
	                "	     <arg var=\"In\">\n"+
	                "		 <featurePath root=\"down\" dimension=\"D\" aspect=\"entry\">\n"+
	                "		     <constant data=\"in\"/>\n"+
	                "		 </featurePath>\n"+
	                " 	     </arg>\n"+
	                " 	 </usePrinciple>\n"+
	                "	 <usePrinciple idref=\"principle.out\">\n"+
	                "	     <dim var=\"D\" idref=\"id\"/>\n"+
	                "	     <arg var=\"Out\">\n"+
	                "		 <featurePath root=\"down\" dimension=\"D\" aspect=\"entry\">\n"+
	                "		     <constant data=\"out\"/>\n"+
	                "		 </featurePath>\n"+
	                "	     </arg>\n"+
	                "	 </usePrinciple>\n"+
	                "	 <output idref=\"output.pretty\"/>\n\n"+
	                "    </dimension>\n"+
	                
	                
	                
	                /************* DIMENSION: lp ***********/
	                "    <dimension id=\"lp\">\n"+
	                " 	 <attrsType>\n"+
	                "	     <typeRecord>\n\n"+
	                "	     </typeRecord>\n\n"+
	                "	 </attrsType>\n"+
	                "	 <entryType>\n"+
	                "	     <typeRecord>\n"+
	                "		 <typeFeature data=\"in\">\n"+
	                "		     <typeISet>\n"+
	                "			 <typeRef idref=\"lp.label\"/>\n"+
	                "		     </typeISet>\n"+
	                "		 </typeFeature>\n"+
	                "		 <typeFeature data=\"out\">\n"+
	                "		     <typeValency>\n"+
	                "			 <typeRef idref=\"lp.label\"/>\n"+
	                "		     </typeValency>\n"+
	                "		 </typeFeature>\n"+
	                "		 <typeFeature data=\"on\">\n"+
	                "		     <typeISet>\n"+
	                "			 <typeRef idref=\"lp.label\"/>\n"+
	                "		     </typeISet>\n"+
	                "		 </typeFeature>\n"+
	                "	     </typeRecord>\n"+
	                "	 </entryType>\n"+
	                "	 <labelType>\n"+
	                "	     <typeRef idref=\"lp.label\"/>\n"+
	                "	 </labelType>\n"+
	                "	 <typeDef id=\"lp.label\">\n"+
	        "	     <typeDomain>\n");
	        
	        //sb.append("	      <constant data=\"sentence\"/>\n");
	        sb.append("	      <constant data=\"dum\"/>\n");
	        //<!-- HIER: alle Knotenadressen (+ ggf. _L/_R) -->\n
	        for (String ad : addresses) {
	            sb.append("	      <constant data=\""+ad+"\"/>\n");
	        }
	        
	        sb.append("	    </typeDomain>\n"+
	                "        </typeDef>\n"+
	                "	 <usePrinciple idref=\"principle.graph1\">\n"+
	                "	     <dim var=\"D\" idref=\"lp\"/>\n\n"+
	                "	 </usePrinciple>\n"+
	                "	 <usePrinciple idref=\"principle.tree\">\n"+
	                "	     <dim var=\"D\" idref=\"lp\"/>\n\n"+
	                "	 </usePrinciple>\n"+
	                "	 <usePrinciple idref=\"principle.in\">\n"+
	                "	     <dim var=\"D\" idref=\"lp\"/>\n"+
	                "	     <arg var=\"In\">\n"+
	                "		 <featurePath root=\"down\" dimension=\"D\" aspect=\"entry\">\n"+
	                "		     <constant data=\"in\"/>\n"+
	                "		 </featurePath>\n"+
	                "	     </arg>\n"+
	                "	 </usePrinciple>\n"+
	                "	 <usePrinciple idref=\"principle.out\">\n"+
	                "	     <dim var=\"D\" idref=\"lp\"/>\n"+
	                "	     <arg var=\"Out\">\n"+
	                "		 <featurePath root=\"down\" dimension=\"D\" aspect=\"entry\">\n"+
	                "		     <constant data=\"out\"/>\n"+
	                " 		 </featurePath>\n"+
	                "	     </arg>\n"+
	                "	 </usePrinciple>\n"+
	                "	 <usePrinciple idref=\"principle.order\">\n"+
	                "	     <dim var=\"D\" idref=\"lp\"/>\n"+
	                "	     <arg var=\"On\">\n"+
	                "		 <featurePath root=\"down\" dimension=\"D\" aspect=\"entry\">\n"+
	                "		     <constant data=\"on\"/>\n"+
	                "		 </featurePath>\n"+
	                "	     </arg>\n"+
	                "	     <arg var=\"Order\">\n"+
	        "		 <list>\n");
	        
	        //<!-- HIER: die Reihenfolge der LP-Label -->\n
	        for (String ad : addresses) {
	            sb.append("	      <constant data=\""+ad+"\"/>\n");
	        }
	        sb.append("	      <constant data=\"dum\"/>\n");
	        
	        sb.append("		 </list>\n"+
	                "	     </arg>\n"+
	                "	     <arg var=\"Projective\">\n"+
	                "		 <constant data=\"true\"/>\n"+
	                "	     </arg>\n"+
	                "	     <arg var=\"Yields\">\n"+
	                "		 <constant data=\"true\"/>\n"+
	                "	     </arg>\n"+
	                "	 </usePrinciple>\n"+
	                "	 <usePrinciple idref=\"principle.parse\">\n"+
	                "	     <dim var=\"D\" idref=\"lp\"/>\n\n"+
	                "	 </usePrinciple>\n"+
	                "	 <output idref=\"output.pretty\"/>\n\n"+
	                "    </dimension>\n"+
	                
	                
	                
	                /********** DIMENSION: idlp **************/
	                
	                "    <dimension id=\"idlp\">\n"+
	                "	 <attrsType>\n"+
	                "	     <typeRecord>\n\n"+
	                "	     </typeRecord>\n"+
	                " 	 </attrsType>\n"+
	                "	 <entryType>\n"+
	                "	     <typeRecord>\n"+
	                "		 <typeFeature data=\"link\">\n"+
	                "		     <typeMap>\n"+
	                "			 <typeRef idref=\"lp.label\"/>\n"+
	                "			 <typeSet>\n"+
	                "			     <typeRef idref=\"id.label\"/>\n"+
	                "			 </typeSet>\n"+
	                "		     </typeMap>\n"+
	                "		 </typeFeature>\n"+
	                "	     </typeRecord>\n"+
	                "	 </entryType>\n"+
	                "	 <labelType>\n"+
	                "	     <typeDomain>\n\n"+
	                " 	     </typeDomain>\n"+
	                "	 </labelType>\n\n"+
	                "	 <usePrinciple idref=\"principle.linkingDaughterEnd\">\n"+
	                "	     <dim var=\"D1\" idref=\"lp\"/>\n"+
	                "	     <dim var=\"D2\" idref=\"id\"/>\n"+
	                "	     <arg var=\"End\">\n"+
	                "		 <featurePath root=\"up\" dimension=\"This\" aspect=\"entry\">\n"+
	                "		     <constant data=\"link\"/>\n"+
	                "		 </featurePath>\n"+
	                "	     </arg>\n"+
	                "	 </usePrinciple>\n"+
	                "	 <output idref=\"output.pretty\"/>\n\n"+
	                "    </dimension>\n"+
	                
	                
	                
	                /************** DIMENSION: lex ***************/
	                
	                "    <dimension id=\"lex\">\n"+
	                "	 <attrsType>\n"+
	                " 	     <typeRecord>\n\n"+
	                "	     </typeRecord>\n"+
	                "	 </attrsType>\n"+
	                "	 <entryType>\n"+
	                "	     <typeRecord>\n"+
	                "		 <typeFeature data=\"word\">\n"+
	                "		     <typeString/>\n"+
	                "		 </typeFeature>\n"+
	                "		 <typeFeature data=\"anchor\">\n"+
	                "		     <typeString/>\n"+
	                "		 </typeFeature>\n"+
	                "		 <typeFeature data=\"span\">\n"+
	                "		     <typeInts/>\n"+
	                "		 </typeFeature>\n"+
	                "	     </typeRecord>\n"+
	                " 	 </entryType>\n"+
	                "	 <labelType>\n"+
	                "	     <typeDomain>\n\n"+
	                "            </typeDomain>\n"+
	                "	 </labelType>\n"+
	                "    <usePrinciple idref=\"principle.poetry\">\n"+
	                "        <dim var=\"D\" idref=\"lex\"/>\n"+
	                "    </usePrinciple>\n\n"+
	                "	 <output idref=\"output.dags1\"/>\n"+
	                "	 <output idref=\"output.latexs1\"/>\n"+
	                "	 <useOutput idref=\"output.dags1\"/>\n"+
	                "    </dimension>\n");
	    }
	    
	    public void printEntry (Writer sb, XDGEntry entry, Node tree,
	    						int startPos, int numSyllables) throws IOException {
	        sb.append("     <entry> <!-- " + tree + " -->\n");
	        sb.append("	 <classConj>\n"+
	                "	     <classDimension idref=\"id\">\n"+
	                "		 <record>\n"+
	                
	                
	                
	                /** incoming ID valency **/
	                
	                "		     <feature data=\"in\">\n"+
	                "			 <set>\n");
	        sb.append("		            <constant data=\""+entry.rootCat+"\"/>\n");
	        sb.append("			 </set>\n"+
	                "		     </feature>\n"+
	                
	                
	                
	                /** outgoing ID valency **/
	                
	                "		     <feature data=\"out\">\n"+
	        "			<set>\n");
	        int elCounter = 1;
	        int subCounter = 0;
	        for (int i = 0; i < entry.outId.size(); i++){
	            String next = entry.outId.get(i);
	            String[] adStrings = next.split("_");
	            String label = adStrings[0];
	            for (int j = i+1; j < entry.outId.size(); j++){
	                String element = entry.outId.get(j);
	                String[] elStrings = element.split("_");
	                String elLabel = elStrings[0];
	                if (label.equals(elLabel)){
	                    elCounter++;
	                    if (elStrings.length == 2){
	                        subCounter++;}
	                    entry.outId.remove(j);}
	            }
	            
	            if (elCounter>1){
	                sb.append("                           <constantCardSet data=\""+
	                        label+"\">\n");
	                for (int k = subCounter; k<=elCounter;k++){
	                    sb.append("                                   <integer data=\""+
	                            k+"\"/>\n");
	                }
	                sb.append("                           </constantCardSet>\n");
	                elCounter = 1;
	                subCounter = 0;
	            }
	            else{
	                String opt = "";
	                if (adStrings.length == 2){
	                    opt = "one";}
	                else {opt = "opt";}
	                sb.append("                           <constantCard data=\""+
	                        label+"\" card=\""+opt+"\"/>\n");}
	        } 
	        
	        sb.append("		        </set>\n"+
	                "		     </feature>\n"+
	                "		 </record>\n"+
	                "	     </classDimension>\n"+

	                
	                
	                
	                
	                
	                "	     <classDimension idref=\"lp\">\n"+
	                "		 <record>\n"+

	                
	                /** incoming LP valency **/
	                
	        "		     <feature data=\"in\">\n");
	        sb.append(" 	  	        <set>\n");
	        for (String ad : entry.inLp){
	            sb.append("                         <constant data=\""
	                    +ad+"\"/>\n");}
	        sb.append("                     </set>\n"+
	                "		    </feature>\n"+
	                
	                
	                /** outgoing LP valency **/
	                
	                "		     <feature data=\"out\">\n"+
	        "			 <set>\n");
	        for (String ad : entry.outLp){
	            String[] adStrings = ad.split("_");
	            String label = adStrings[0];
	            String opt = "";
	            if (adStrings.length == 2){
	                opt = "one";}
	            else {opt = "opt";}
	            sb.append("	   	               <constantCard data=\""+label+"\" card=\""+opt+"\"/>\n");}
	        sb.append("			      </set>\n"+
	                "		     </feature>\n"+
	                
	                
	                /** "on" LP valency **/
	                
	                "		     <feature data=\"on\">\n"+
	        "			 <set>\n");
	        sb.append("       	        <constant data=\""+entry.anchorAddress+"\"/>\n");
	        sb.append("       	         </set>\n"+
	                "		     </feature>\n"+
	                "		 </record>\n"+
	                "	     </classDimension>\n"+

	                
	                
	                
	                
	                "	     <classDimension idref=\"idlp\">\n"+
	                "		 <record>\n"+
	                
	                
	                /** ID/LP Link feature **/
	                
	                "		     <feature data=\"link\">\n"+
	        "			 <record>\n");
	        for (String add : entry.linking.keySet()){
	            sb.append("                            <feature data=\""+add+"\">\n"+
	            "                                <set>\n");
	            for (String cat : entry.linking.get(add)){
	                sb.append("                                    <constant data=\""+cat+"\"/>\n");}
	            sb.append("                                </set>\n"+
	            "                            </feature>\n");
	        }
	        sb.append("			 </record>\n"+
	                "		     </feature>\n"+
	                "		 </record>\n"+
	                "	     </classDimension>\n"+
	                
	                
	                
	                
	                
	                
	                
	                "	     <classDimension idref=\"lex\">\n"+
	                "		 <record>\n"+
	                

	                /** LEX: word (this is always "a") **/
	                
	                "		     <feature data=\"word\">\n" +
	                "                <constant data=\"a\" />\n" +
	                "            </feature>\n" +
	                
	                
	                /** LEX: anchor **/
	                
	                "            <feature data=\"anchor\">\n" +
	                "                       <constant data=\""+entry.anchor+"\"/>\n" +
	                " 		     </feature>\n"+
	                
	                
	                /** LEX: span **/
	                
	                "            <feature data=\"span\">\n" +
	                "               <set>\n");
	        
	        for( int i = startPos+1; i < startPos + numSyllables + 1; i++ ) {
	        	sb.append("               <integer data=\"" + i + "\"/>\n");
	        }
	        
	        sb.append("               </set>\n" +
	        		" 		     </feature>\n"+
	                "		 </record>\n"+
	                "	     </classDimension>\n"+
	                " 	 </classConj>\n"+
	        "     </entry>\n\n\n");
	    }
	    
	    public void printEnd (Writer sb, int syllables) throws IOException {
	    	
	    	/************ dummy lexicon entry for "a" ***********/
	    	sb.append("  <entry>\n" +
	    			"    <classConj>\n" +
	    			"      <classDimension idref=\"id\">\n" +
	    			"        <record>\n" +
	    			"          <feature data=\"in\">\n" +
	    			"            <set>\n" +
	    			"              <constant data=\"dum\"/>\n" +
	    			"            </set>\n" +
	    			"          </feature>\n" +
	    			"          <feature data=\"out\">\n" +
	    			"            <set>\n" +
	    			"            </set>\n" +
	    			"          </feature>\n" +
	    			"        </record>\n" +
	    			"      </classDimension>\n" +
	    			"      <classDimension idref=\"lp\">\n" +
	    			"        <record>\n" +
	    			"          <feature data=\"in\">\n" +
	    			"            <set>\n" +
	    			"              <constant data=\"dum\"/>\n" +
	    			"            </set>\n" +
	    			"          </feature>\n" +
	    			"          <feature data=\"on\">\n" +
	    			"            <set>\n" +
	    			"              <constant data=\"dum\"/>\n" +
	    			"            </set>\n" +
	    			"          </feature>\n" +
	    			"          <feature data=\"out\">\n" +
	    			"            <set>\n" +
	    			"            </set>\n" +
	    			"          </feature>\n" +
	    			"        </record>\n" +
	    			"      </classDimension>\n" +
	    			"      <classDimension idref=\"lex\">\n" +
	    			"        <record>\n" +
	    			"          <feature data=\"word\">\n" +
	    			"            <constant data=\"a\"/>\n" +
	    			"          </feature>\n" +
	    			"          <feature data=\"anchor\">\n" +
	    			"            <constant data=\"#\"/>\n" +
	    			"          </feature>\n" +
	    			"          <feature data=\"span\">\n" +
	    			"            <set>\n" +
	    			"            </set>\n" +
	    			"          </feature>\n" +
	    			"        </record>\n" +
	    			"      </classDimension>\n" +
	    			"    </classConj>\n" +
	    			"  </entry>\n\n\n");
	    	
	    	
	    	/***** lexicon entry for "." (from Stefan's encoding) ******/
	    	
	        sb.append("  <entry>\n"+
	                "	 <classConj>\n"+
	                "	     <classDimension idref=\"id\">\n"+
	                "		 <record>\n"+
	                "		     <feature data=\"in\">\n"+
	                "		         <set>\n"+
	                "			  </set>\n"+
	                "		     </feature>\n"+
	                "		     <feature data=\"out\">\n"+
	                "			<set>\n"+
	                "                    <constantCard data=\"S\" card=\"one\"/>\n"+
	        		"                    <constantCard data=\"dum\" card=\"any\"/>\n" +
	                "		        </set>\n"+
	                "		     </feature>\n"+
	                "		 </record>\n"+
	                "	     </classDimension>\n"+
	                "	     <classDimension idref=\"lp\">\n"+
	                "		 <record>\n"+
	                "		     <feature data=\"in\">\n"+
	                " 	  	        <set>\n"+
	                "                     </set>\n"+
	                "		    </feature>\n"+
	                "		     <feature data=\"out\">\n"+
	                "			   <set>\n"+
	                "	   	          <constantCard data=\"M.1\" card=\"one\"/>\n"+
	        		"                 <constantCard data=\"dum\" card=\"any\"/>\n" +
	                "			   </set>\n"+
	                "		     </feature>\n"+
	                "		     <feature data=\"on\">\n"+
	                "			 <set>\n"+
	                "       	        <constant data=\"M.2\"/>\n"+
	                "       	         </set>\n"+
	                "		     </feature>\n"+
	                "		 </record>\n"+
	                "	     </classDimension>\n"+
	                "	     <classDimension idref=\"idlp\">\n"+
	                "		 <record>\n"+
	                "		     <feature data=\"link\">\n"+
	                "			 <record>\n"+
	                "                            <feature data=\"M.1\">\n"+
	                "                                <set>\n"+
	                "                                    <constant data=\"S\"/>\n"+
	                "                                </set>\n"+
	                "                            </feature>\n"+
	                "                            <feature data=\"dum\">\n"+
	                "                                <set>\n"+
	                "                                    <constant data=\"dum\"/>\n"+
	                "                                </set>\n"+
	                "                            </feature>\n"+
	                "			 </record>\n"+
	                "		     </feature>\n"+
	                "		 </record>\n"+
	                "	     </classDimension>\n"+
	                "	     <classDimension idref=\"lex\">\n"+
	                "		 <record>\n"+
	                "		     <feature data=\"word\">\n"+
	                "                       <constant data=\".\"/>\n"+
	                " 		     </feature>\n"+
	                "		     <feature data=\"anchor\">\n"+
	                "                       <constant data=\".\"/>\n"+
	                " 		     </feature>\n"+
	                "		     <feature data=\"span\">\n"+
	    			"            <set>\n" +
	    			"                <integer data=\"" + (syllables+1) + "\" />\n" +
	    			"            </set>\n" +
	                " 		     </feature>\n"+
	                "		 </record>\n"+
	                "	     </classDimension>\n"+
	                " 	 </classConj>\n"+
	                "     </entry>\n\n\n");
	        
	        
	        /************ lexicon entry for $ (Marco's end symbol) *****
	        sb.append("  <entry>\n" +
	        		"    <classConj>\n" +
	        		"      <classDimension idref=\"id\">\n" +
	        		"        <record>\n" +
	        		"          <feature data=\"in\">\n" +
	        		"            <set>\n" +
	        		"            </set>\n" +
	        		"          </feature>\n" +
	        		"          <feature data=\"out\">\n" +
	        		"            <set>\n" +
	        		"              <constant data=\"sentence\"/>\n" +
	        		"              <constantCard data=\"dum\" card=\"any\"/>\n" +
	        		"            </set>\n" +
	        		"          </feature>\n" +
	        		"        </record>\n" +
	        		"      </classDimension>\n" +
	        		"      <classDimension idref=\"lp\">\n" +
	        		"        <record>\n" +
	        		"          <feature data=\"in\">\n" +
	        		"            <set>\n" +
	        		"            </set>\n" +
	        		"          </feature>\n" +
	        		"          <feature data=\"on\">\n" +
	        		"            <set>\n" +
	        		"              <constant data=\"a\"/>\n" +
	        		"            </set>\n" +
	        		"          </feature>\n" +
	        		"          <feature data=\"out\">\n" +
	        		"            <set>\n" +
	        		"              <constant data=\"sentence\"/>\n" +
	        		"              <constantCard data=\"dum\" card=\"any\"/>\n" +
	        		"            </set>\n" +
	        		"          </feature>\n" +
	        		"        </record>\n" +
	        		"      </classDimension>\n" +
	        		"      <classDimension idref=\"lex\">\n" +
	        		"        <record>\n" +
	        		"          <feature data=\"word\">\n" +
	        		"            <constant data=\"$\"/>\n" +
	        		"          </feature>\n" +
	        		"          <feature data=\"span\">\n" +
	        		"            <set>\n" +
	        		"              <integer data=\"" + (syllables+2) + "\"/>\n" +
	        		"            </set>\n" +
	        		"          </feature>\n" +
	        		"        </record>\n" +
	        		"      </classDimension>\n" +
	        		"    </classConj>\n" +
	        		"  </entry>\n\n\n");
	        		*/

	    
	        sb.append("  </grammar>\n");
	    }
}
