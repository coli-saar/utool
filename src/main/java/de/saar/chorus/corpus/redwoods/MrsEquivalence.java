package de.saar.chorus.corpus.redwoods;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.saar.chorus.corpus.redwoods.MrsComparator.MRStriple;
import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;

public class MrsEquivalence {

	private CodecManager man;
	private File corpusDir;
	public MrsEquivalence(String corpus) {
		man = new CodecManager(); 
		corpusDir = new File(corpus);
		File analysis = new File(corpus.replace("/mrs", "/mrsanalysis") );
		analysis.mkdir();
		try {
			man.registerAllDeclaredCodecs();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}


	public void checkCorpus() throws IOException {

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


		System.err.println();
		System.err.println("======================================================================");
		System.err.println("Folder " + corpusDir.getAbsolutePath() + " (" + sentences.length + " parses): ");
	
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



		Map<String, List<String>> mrsesToParses = new HashMap<String,List<String>>();
		Map<MRStriple, List<String>> mrsFullToParses = new HashMap<MRStriple, List<String>>();
		//	Set<String> mrses = new HashSet<String>();
		//	Set<MRStriple> finalmirses = new HashSet<MRStriple>();
		int malformed = 0, otex = 0;
		for(File file : files) {
			System.err.println("checking " + file.getName() + "...");
			StringBuffer nextmrs = new StringBuffer(500);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while(line != null) {
				nextmrs.append(line);
				line = reader.readLine();
			}

			String mrsstring = nextmrs.toString();
			System.err.print("Read MRS...");
			List<String> parsenumbers = mrsesToParses.get(mrsstring);

			if(parsenumbers == null) {
				parsenumbers = new ArrayList<String>();
				mrsesToParses.put(mrsstring, parsenumbers);
			}
			System.err.print("In String-Map...");

			parsenumbers.add(file.getName());
			//		mrses.add(nextmrs.toString());

		}

		System.err.println("Full matching: ");
		for(String mrs : mrsesToParses.keySet()) {
			System.err.print("MRS " + mrsesToParses.get(mrs));
			MRStriple trio = new MRStriple(mrs);
			try {
				long time = System.currentTimeMillis();
				trio.readAndSolve();
				System.err.print("Solved...(" + (System.currentTimeMillis() - time) + "ms)");
				List<String> parses = mrsFullToParses.get(trio);
				if(parses == null) {
					parses = new ArrayList<String>();
					mrsFullToParses.put(trio, parses);
				} else {
					trio.addMRSString(mrs);
				}
			
				parses.addAll(mrsesToParses.get(mrs));
				System.err.println("Compared!");

				//finalmirses.add(trio);
			}
			catch( MalformedDomgraphException e) {
					System.err.println("Mf DG Exc.!");
				malformed++;
				continue;
			} catch( Exception e) {
					System.err.println("Parser or IO");
				otex++;
				e.printStackTrace();
				continue;
			}
		}

		writeLog(mrsesToParses, mrsFullToParses, dir);


		System.err.println(dir.getName() + " has " + files.length + " parses and " + mrsesToParses.size() + 
				" different USRs strings and " + mrsFullToParses.size() + " valid USR charts (."
				+ malformed + " mf, " + otex + "other exc.)");




		List<Integer> ret = new ArrayList<Integer>();
		ret.add(files.length);
		ret.add(mrsesToParses.size());
		ret.add(mrsFullToParses.size());
		ret.add(malformed);
		ret.add(otex);
		return ret;
	}


	private static void writeLog(Map<String, List<String>> mrses1, Map<MRStriple, List<String>>
	mrses2, File folder) throws IOException {
		
		
	//	File storage = new File(folder.getAbsolutePath().replace("/mrs", "/mrsanalysis"));
	//	storage.createNewFile();
		FileWriter filelog = new FileWriter(folder.getAbsolutePath().replace("/mrs", "/mrsanalysis"));
		

		String n = System.getProperty("line.separator");


		filelog.append("String matching: " + n + n);
		for( Map.Entry<String, List<String>> stringpair : mrses1.entrySet() ) {
			filelog.append(stringpair.getKey() + ":" + ": (" + stringpair.getValue().size() +")"+ n);
			for(String parse : stringpair.getValue()) {
				filelog.append(parse + n );
			}
			filelog.append( n + n );
			filelog.flush();
		}

		filelog.append( n + n );
		filelog.append("Full matching:" + n +n);

		for( Map.Entry<MRStriple, List<String>> fullpair : mrses2.entrySet() ) {
			Set<String> mrses = fullpair.getKey().allStrings;
			int i = 1;
			for(String usr : mrses) {
				
				filelog.append("[" + i + "] " + usr + n);
				i++;
				
			}
			
			filelog.append("(" + fullpair.getValue().size() +")" + n);
			for(String parse : fullpair.getValue()) {
				filelog.append(parse + n );
			}
			filelog.append( n + n );
			filelog.flush();
		}

		filelog.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String redwoods = "/Users/Michaela/Uni/Resources/Redwoods_tsdb_export/redwoods.jan-06.jh4.06-01-30.";
		MrsEquivalence mrse = new MrsEquivalence(redwoods + "/mrs");
		
		try {
			checkFolder(new File("/Users/Michaela/Uni/Resources/Redwoods_tsdb_export/redwoods.jan-06.jh4.06-01-30./mrs/41503"));
		//	mrse.checkCorpus();
		} catch(Exception e) {
			e.printStackTrace();
		}

	}

}
