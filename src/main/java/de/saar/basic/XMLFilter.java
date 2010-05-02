package de.saar.basic;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * A <code>FileFilter</code> that 
 * accepts files with *.xml-extension.
 * 
 * @author Michaela Regneri
 *
 */
public class XMLFilter extends FileFilter {
	
	/**
	 * Overwrites the <code>accept</code> method
	 * of <code>Filefilter</code>.
	 * 
	 * @return true if the file has an xml extension
	 */
	public boolean accept(File f) {
		String fileName = f.getName();
		if( f.isDirectory() ) {
			return true;
		} 
		if(fileName.indexOf(".xml") > 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * Overwrites the <code>getDescription</code> 
	 * method of <code>FileFilter</code>.
	 * 
	 * @return just "XML"
	 */
	public String getDescription() {
		
		return "XML";
	}
}