package de.saar.basic;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * TODO comment me!
 * 
 * @author Alexander Koller
 *
 */
public class GenericFileFilter extends FileFilter implements Comparable {
	private String extension;
	private String desc;
	
	/**
	 * 
	 * @param extension
	 * @param desc
	 */
	public GenericFileFilter(String extension, String desc) {
		if( extension.startsWith(".") )
			this.extension = extension;
		else
			this.extension = "." + extension;
		
		this.desc = desc;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean accept(File f) {
		String fileName = f.getName();
		
		if( f.isDirectory() ) {
			return true;
		} 
		
		if(fileName.endsWith(extension) ) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * 
	 * @return
	 */
    @Override
	public String getDescription() {
		return desc + " files (*" + extension + ")";
	}
    
	public String getName() {
		return desc;
	}
	
	public String getExtension() {
		return extension;
	}
	
	public int compareTo(Object o) {
		return desc.compareTo( 
				((GenericFileFilter)o).desc);
	}
	
}