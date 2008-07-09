package de.saar.chorus.corpus.redwoods;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class MrsExporter {

	private static String numberIndicator = "\\[\\d+:(\\d+)\\]\\W\\(([a-z]+)\\)";
	private static Pattern numberPattern = Pattern.compile(numberIndicator);
	public static void exportDirectory(String directory) throws IOException {
		File dir = new File(directory);
		exportDirectory(dir);
		
	}
	
	public static void exportDirectory(File dir) throws IOException {
		if(! dir.isDirectory() ) {
			throw new IOException("The given path does not point to a directory.");
		} else {
			File[] contents = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if(name.endsWith(".gz")) {
						return true;
					} else {
						return false;
					}
				}
			});
			
			for(File parse : contents) {
				
				exportFile(parse,parse.getParent() + 
						"/mrs/"+ 
						parse.getName().substring(0, parse.getName().length() - 3));
			}
		}
	}
 	
	public static void exportFile(File file, String destination) throws IOException {
		
		System.err.println("Extracting from " + file.getName() + " into " + destination + "...");
		
		File dest = new File(destination);
		if(! dest.isDirectory()) {
			System.err.println(dest.mkdirs());
		}
		
		GZIPInputStream zipin = new GZIPInputStream(new FileInputStream(file));
		BufferedReader reader = new BufferedReader(new InputStreamReader(zipin));
		String line = reader.readLine();
		
		String lastParse = "";
		
		
		while(line != null) {
			Matcher matchNumber = numberPattern.matcher(line);
			if(matchNumber.find()) {
				lastParse = matchNumber.group(1) + "_" + matchNumber.group(2);
				System.err.println("Next parse is " + matchNumber.group(2) + ", number " + matchNumber.group(1));
			} else {
				if(line.startsWith("psoa")) {
					FileWriter writer = new FileWriter(new File(destination
							+ File.separator + lastParse + ".mrs.pl"));
					System.err.println(">>> MRS:  " + line);
					writer.write(line);
					writer.close();
				}
			}
			line = reader.readLine();
		}
		reader.close();
		zipin.close();
	}
	
	/**
	 * DEBUG
	 * @param args
	 */
	public static void main(String[] args) {
		
		String redwoods = "/Users/Michaela/Uni/Resources/Redwoods_tsdb_export/redwoods.jan-06.jh3.06-01-30.";
		File srcdir = new File(redwoods);
		File[] files = srcdir.listFiles();
		
		
		try {
			exportDirectory(redwoods);
		
		} catch( Exception e) {
			e.printStackTrace();
		}
	}
	
}
