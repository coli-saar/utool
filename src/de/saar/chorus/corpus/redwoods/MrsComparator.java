package de.saar.chorus.corpus.redwoods;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;
import de.saar.chorus.domgraph.chart.modelcheck.ModelCheck;
import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.OutputCodec;
import de.saar.chorus.domgraph.codec.ParserException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class MrsComparator {
	private static CodecManager man;
	private static BufferedWriter log;
	static {
		man = new CodecManager(); try {
		man.registerAllDeclaredCodecs();
		log =   new BufferedWriter(new FileWriter("mrs_jh3.stats", true));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void checkCorpus(File corpusDir) throws IOException {
		
		log.append("File\t\t parses\t mrs_string\t mrs_chart\t malformed\t other err.\n");
		double averageReadings = 1.0, averageMRSes = 1.0, averageCharts = 1.0, 
			averageMF = 1.0, averageOther = 1.0;
		int singletons = 0;
		File[] sentences = corpusDir.listFiles();
		for(File file : sentences) {
			if(file.isDirectory() && (file.list().length < 300 ) ) {
				List<Integer>result = checkFolder(file);
				averageReadings += (double) result.get(0);
				averageMRSes += (double) result.get(1); 
				averageCharts += (double) result.get(2);
				averageMF += (double) result.get(3);
				averageOther += (double) result.get(4);
				
				if(result.get(0) == 1) {
					singletons++;
				}
			}
		}
		
		averageReadings = averageReadings/ (double) sentences.length;
		averageMRSes = averageMRSes / (double) sentences.length;
		averageCharts = averageCharts / (double) sentences.length;
		averageMF = averageMF / (double) sentences.length;
		averageOther = averageOther / (double) sentences.length;
		
		
		log.flush();
		log.close();
		
		System.err.println();
		System.err.println("======================================================================");
		System.err.println("Folder : " + corpusDir.getAbsolutePath());
		System.err.println(averageReadings + " readings on average.");
		System.err.println(averageMRSes + " MRS Strings on average (" + singletons + " singletons).");
		System.err.println(averageCharts + " MRS Charts on average (" + averageMF + " malformed, " + 
				averageOther + " o.ex.");
	} 
	
	public static List<Integer> checkFolder(File dir) throws IOException {
		
		File[] files = dir.listFiles(new FilenameFilter() {

			public boolean accept(File dir, String name) {
				
				return name.endsWith("mrs.pl");
			}
			
		});
		Set<String> mrses = new HashSet<String>();
		Set<MRStriple> finalmirses = new HashSet<MRStriple>();
		int malformed = 0, otex = 0;
		for(File file : files) {
			StringBuffer nextmrs = new StringBuffer(100000);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while(line != null) {
				nextmrs.append(line);
				line = reader.readLine();
			}
			mrses.add(nextmrs.toString());
			
		}
		
		for(String mrs : mrses) {
			MRStriple trio = new MRStriple(mrs);
			try {
			trio.readAndSolve();
			finalmirses.add(trio);
			}
			catch( MalformedDomgraphException e) {
			//	System.err.println("Mf DG Exc.!");
				malformed++;
				continue;
			} catch( Exception e) {
			//	System.err.println("Parser or IO");
				otex++;
				continue;
			}
		}
		log.append(dir.getName() + "\t" + files.length + "\t\t" + mrses.size() + "\t\t" + finalmirses.size() + "\t\t"
				+ malformed + "\t" + otex + "\n");
		log.flush();
		System.err.println(dir.getName() + " has " + files.length + " parses and " + mrses.size() + 
				" different USRs strings and " + finalmirses.size() + " valid USR charts (."
				+ malformed + " mf, " + otex + "other exep.)");
		List<Integer> ret = new ArrayList<Integer>();
		ret.add(files.length);
		ret.add(mrses.size());
		ret.add(finalmirses.size());
		ret.add(malformed);
		ret.add(otex);
		return ret;
	}
	
	public static void exportCorpus(File folder) throws IOException {
		double allReadings = 0.0, nothnv = 0.0, notwf=0.0, notparseable = 0.0, 
			notsolvable = 0.0, skipped = 0.0;
		
		File[] sentences = folder.listFiles();
		for(File file : sentences) {
			if(file.isDirectory() && (file.getName().compareTo("30103") > 0)) {
				List<Integer> stats = exportFolder(file);
				notwf += (double) stats.get(1);
				nothnv += (double) stats.get(2);
				notparseable += (double) stats.get(3);
				allReadings += (double) stats.get(0); 
				notsolvable += (double) stats.get(4);
				skipped += (double) stats.get(5);
				
			}
		}
		String n = System.getProperty("line.separator");
		BufferedWriter statwriter = new BufferedWriter(new FileWriter(new File(folder.getAbsolutePath()
				 +"/domcon-stats")));
		statwriter.append("MRSes in Corpus:\t\t" + (int) allReadings+ n );
		statwriter.append("Not parseable:\t\t" + (int) notparseable + "(" +
				(notparseable * 100.0/allReadings) + "%)" + n);
		statwriter.append("Not well-formed:\t\t"+ (int) notwf + "(" +
				(notwf * 100.0/allReadings) + "%)" + n);
		statwriter.append("Not hnv:\t\t\t"+ (int) nothnv + "(" +
				(nothnv * 100.0/allReadings) + "%)" + n);
		statwriter.append("Not solvable:\t\t"+ (int) notsolvable + "(" +
				(notsolvable * 100.0/allReadings) + "%)" + n);
		statwriter.append("Skipped (more than 8000 sfs): \t"+ (int) skipped + "(" +
				(skipped * 100.0/allReadings) + "%)" + n);
		statwriter.flush();
		statwriter.close();
		
	}
	
	public static List<Integer> exportFolder(File folder) throws IOException {
		File[] files = folder.listFiles(new FileFilter() {

			public boolean accept(File pathname) {
				return(pathname.getName().endsWith("mrs.pl"));
			}
			
		});
		System.err.println("Exporting sentence in " + folder.getName() + "... (" + files.length + "MRSes)");
		//0: all; 1 : not well-formed; 2: not hnv; 3: parser ex; 4: not solvable; 5: skipeed (too big)
		List<Integer> stats = new ArrayList<Integer>();
		int all = files.length, nwf = 0, nhnv = 0, pe = 0, nsolvable = 0, skip = 0;
		for( File file : files ) {
			DomGraph dg = new DomGraph();
			NodeLabels nl = new NodeLabels();
			FileInputStream in = new FileInputStream(file);
			InputCodec mrsin = man.getInputCodecForFilename(file.getName(), new HashMap<String,String>());
			try {
				mrsin.decode(new InputStreamReader(in), dg, nl);
				if(! dg.isHypernormallyConnected()) {
					nhnv++;
				}
				Chart chart = new Chart(nl);
				try {
					if (! ChartSolver.solve(dg, chart) ) {
						nsolvable++;
					} else {
						BigInteger noofforms = chart.countSolvedForms();
						System.err.println("MRS for parse " + file.getName().substring(0, 1) + " - " +noofforms + "SFS...");
						if(noofforms.compareTo(BigInteger.valueOf(8000)) > 0) {
							System.err.println("Too big! Skipping.");
							skip++;
						} else {
						SolvedFormIterator<SubgraphNonterminal> it = 
							new SolvedFormIterator<SubgraphNonterminal>(chart, dg);
						String filename = file.getAbsolutePath() + "_sfs.t.oz";
						FileWriter writer = new FileWriter(new File(filename));
						OutputCodec out = man.getOutputCodecForFilename(filename, new HashMap<String,String>());
						while(it.hasNext()) {
							DomGraph nextsf = dg.makeSolvedForm(it.next());
							out.encode(nextsf, nl, writer);
						}
						writer.close();
						}
					}

				} catch( Exception e) {
					e.printStackTrace();
					nsolvable++;
				}
			} catch (ParserException e) {
				e.printStackTrace();
				pe++;
			} catch( MalformedDomgraphException e ) {
				nwf++;
			}
		}

		stats.add(all);
		stats.add(nwf);
		stats.add(nhnv);
		stats.add(pe);
		stats.add(nsolvable);
		stats.add(skip);
		return stats;

	}
	// start again with 30103
	
	public static void main(String[] args) {
		String redwoods = "/Users/Michaela/Uni/Resources/Redwoods_tsdb_export/redwoods.jan-06.jh3.06-01-30.";
		try {
			checkCorpus(new File(redwoods + "/mrs"));
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static class MRStriple {
		String mrsstring;
		Set<String> allStrings;
		DomGraph dg;
		Chart chart;
		NodeLabels labels;
		int hashCode;
		
		
		MRStriple(String mrs) {
			mrsstring = mrs;
			dg = new DomGraph();
			labels = new NodeLabels();
			hashCode = -1;
			allStrings = new HashSet<String>();
			allStrings.add(mrsstring);
		}
		
		void readAndSolve() throws MalformedDomgraphException, ParserException, IOException{
			StringReader in = new StringReader(mrsstring);
			Map<String,String> attrs = new HashMap<String,String>();
			attrs.put("labelStyle", "full");
			InputCodec mrsin = man.getInputCodecForFilename("foo.mrs.pl", attrs);
			
			NodeLabels tmp = new NodeLabels();
			
				mrsin.decode(in, dg, labels);
			
				try {
				
				chart = new Chart(labels);
				ChartSolver.solve(dg, chart);
			} catch( Exception e) {
				e.printStackTrace();
				System.err.println("%% ah, bullshit.");
			}
		}
		
		public String getMRSString() {
			return mrsstring;
		}

		
		public boolean addMRSString(String mrs) {
			return allStrings.add(mrs);
		}
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof MRStriple) {
				MRStriple compare = (MRStriple)obj;
				if(compare.mrsstring.equals(mrsstring)) {
					return true;
				}
				
				if(allStrings.contains(compare.mrsstring)) {
					return true;
				}
				
				if( ModelCheck.subsumes(chart, dg, labels, compare.chart, compare.dg, compare.labels) ) {
				
					allStrings.add(compare.mrsstring);
					compare.allStrings.add(mrsstring);
		
					return true;
				} 
			}
			
			return false;
		}

		@Override
		public int hashCode() {
			
			if(hashCode == -1) {
				hashCode = chart.countSubgraphs() * dg.getAllNodes().size() * dg.getAllEdges().size();
			}
			
			return hashCode;
		}
		
		
	}
}
