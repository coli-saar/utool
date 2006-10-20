/*
 * @(#)CodecManager.java created 27.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import de.saar.chorus.ubench.gui.Ubench;

/**
 * A registry for codecs. Objects of this class are intended as a
 * place where different codec classes can be registered. Then
 * it is possible to look up codecs via their names or their
 * filename extensions. The codec manager will also take care
 * of the correct creation of codec objects; an application will
 * typically never instantiate a codec class itself.
 * 
 * @author Alexander Koller
 *
 */
public class CodecManager {
    /*
     * private List<OutputCodec> outputCodecs;
    private List<InputCodec> inputCodecs;
    */
    
    private List<Class> outputCodecClasses;
    private List<Class> inputCodecClasses;
    private Map<String,File> exampleNameToPath;
    
    public CodecManager() {
        //outputCodecs = new ArrayList<OutputCodec>();
        //inputCodecs = new ArrayList<InputCodec>();
        outputCodecClasses = new ArrayList<Class>();
        inputCodecClasses = new ArrayList<Class>();
        exampleNameToPath = new HashMap<String,File>();
    }
    
    public static String getCodecName(Class codecClass) {
        try {
            Method getName = codecClass.getMethod("getName");
            return (String) getName.invoke(null);
        } catch(Exception e) {
            assert false; // we should never get here
            return null;
        }
    }
    
    public static String getCodecExtension(Class codecClass) {
        try {
            Method getExtension = codecClass.getMethod("getExtension");
            return (String) getExtension.invoke(null);
        } catch(Exception e) {
            assert false; // we should never get here
            return null;
        }
    }
    
    private Object constructCodecArgumentless(Class codecClass) {
        try {
            return codecClass.newInstance();
        } catch (InstantiationException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }
    
    private InputCodec constructInputCodec(Class codecClass, String options)  {
        Object ret = null;
        
        try {
            Constructor con = codecClass.getConstructor(String.class);
            ret = con.newInstance(options);
        } catch(RuntimeException e) {
            ret = constructCodecArgumentless(codecClass);
        } catch(Exception e) {
            ret = constructCodecArgumentless(codecClass);
        } 
        
        try {
            return (InputCodec) ret;
        } catch(ClassCastException e) {
            return null;
        }
    }
    
    private OutputCodec constructOutputCodec(Class codecClass, String options) {
        Object ret = null;
        
        try {
            Constructor con = codecClass.getConstructor(String.class);
            ret = con.newInstance(options);
        } catch(RuntimeException e) {
            ret = constructCodecArgumentless(codecClass);
        } catch(Exception e) {
            ret = constructCodecArgumentless(codecClass);
        }
        
        try {
            return (OutputCodec) ret;
        } catch(ClassCastException e) {
            return null;
        }
    }
    
    
    
    /**
     * Registers a codec. Pass a codec class (not object) as the argument.
     * This class must be a subclass either of {@link InputCodec} or of
     * {@link OutputCodec}, and is filed under the input or output codecs
     * accordingly. 
     * 
     * @param codecClass a codec class
     * @throws CodecRegistrationException if the class is neither a subclass
     * of <code>InputCodec</code> nor of <code>OutputCodec</code>, or if the
     * codec doesn't have a name.
     */
    public void registerCodec(Class codecClass) throws CodecRegistrationException {
        if( getCodecName(codecClass) == null ) {
            throw new CodecRegistrationException("Codec " + codecClass + " has a null name.");
        }
        
        if( OutputCodec.class.isAssignableFrom(codecClass) ) {
            if( constructOutputCodec(codecClass, null) == null ) {
                throw new CodecRegistrationException("Input codec " + codecClass + " has no appropriate constructor");
            }
            
            outputCodecClasses.add(codecClass);
        } else if( InputCodec.class.isAssignableFrom(codecClass)) {
            if( constructInputCodec(codecClass, null) == null ) {
                throw new CodecRegistrationException("Output codec " + codecClass + " has no appropriate constructor");
            }

            inputCodecClasses.add(codecClass);
        } else  {
            throw new CodecRegistrationException("Given codec " + codecClass + " is neither input nor output codec");
        }
    }
    
    /**
     * Gets an input codec object for the input codec with the
     * given name.
     * 
     * @param codecname the name of a registered input codec
     * @return an object of this codec class, or null if no codec
     * with this name was registered.
     */
    public InputCodec getInputCodecForName(String codecname, String options) {
        for( Class codec : inputCodecClasses ) {
            if( codecname.equals(getCodecName(codec)) ) {
                return constructInputCodec(codec, options);
            }
        }
        
        return null;
    }
    
    /**
     * Gets an input codec object for the input codec associated
     * with the given filename (extension).
     * 
     * @param filename the filename for which we need a codec
     * @return an object of this codec class, or null if no codec
     * is associated with this filename extension
     */
    public InputCodec getInputCodecForFilename(String filename, String options) {
        for( Class codec : inputCodecClasses ) {
            String ext = getCodecExtension(codec);
            
            if( ext != null ) {
                if( filename.endsWith(ext) ) {
                    return constructInputCodec(codec, options);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Gets an output codec object for the output codec with the
     * given name.
     * 
     * @param codecname the name of a registered output codec
     * @return an object of this codec class, or null if no codec
     * with this name was registered.
     */
    public OutputCodec getOutputCodecForName(String codecname, String options) {
        for( Class codec : outputCodecClasses ) {
            if( codecname.equals(getCodecName(codec)) ) {
                return constructOutputCodec(codec, options);
            }
        }
        
        return null;
    }
    
    /**
     * Gets an output codec object for the output codec associated
     * with the given filename (extension).
     * 
     * @param filename the filename for which we need a codec
     * @return an object of this codec class, or null if no codec
     * is associated with this filename extension
     */
    public OutputCodec getOutputCodecForFilename(String filename, String options) {
        for( Class codec : outputCodecClasses) {
            String ext = getCodecExtension(codec);
            
            if( ext != null ) {
                if( filename.endsWith(ext) ) {
                    return constructOutputCodec(codec, options);
                }
            }
        }
        
        return null;
    }

    
    /**
     * Prints an overview of all registered codecs to an output stream.
     * 
     * @param out the output stream to which the overview should be printed.
     */
    public void displayAllCodecs(PrintStream out) {
        int max_strlen = 0;
        String formatString, formatStringNoExt;
        
        List<String> inputCodecNames = new ArrayList<String>();
        List<String> outputCodecNames = new ArrayList<String>();
        Map<String,Class> inputCodecClassMap = new HashMap<String,Class>();
        Map<String,Class> outputCodecClassMap = new HashMap<String,Class>();
        
        for( Class codec : inputCodecClasses ) {
            max_strlen = Math.max(max_strlen, getCodecName(codec).length());
            inputCodecNames.add(getCodecName(codec));
            inputCodecClassMap.put(getCodecName(codec), codec);
        }

        for( Class codec : outputCodecClasses ) {
            max_strlen = Math.max(max_strlen, getCodecName(codec).length());
            outputCodecNames.add(getCodecName(codec));
            outputCodecClassMap.put(getCodecName(codec), codec);
        }
        
        formatString = "    %1$-" + max_strlen + "s             (%2$s)";
        formatStringNoExt = "    %1$s";
        
        
        Collections.sort(inputCodecNames);
        Collections.sort(outputCodecNames);
        
        
        out.println("Installed input codecs:");
        for( String inputCodecName : inputCodecNames ) {
            displayOneCodec(inputCodecClassMap.get(inputCodecName), formatString, formatStringNoExt, out);
        }
        
        out.println("\nInstalled output codecs:");
        for( String outputCodecName : outputCodecNames ) {
            displayOneCodec(outputCodecClassMap.get(outputCodecName), formatString, formatStringNoExt, out);
        }
    }

    private void displayOneCodec(Class codec, String formatString, String formatStringNoExt, PrintStream out) {
        String name = getCodecName(codec);
        String ext = getCodecExtension(codec);
        
        if( ext == null ) {
            out.println(String.format(formatStringNoExt, name));
        } else {
            out.println(String.format(formatString, name, ext));
        }
    
        
    }

    /**
     * Returns the list of all registered input codecs.
     * 
     * @return the input codecs
     */
    public List<Class> getAllInputCodecs() {
        return inputCodecClasses;
    }
    
    public Set<String> getAllInputCodecExtensions() {
    	Set<String> extensions = new HashSet<String>();
    	
    	for(Class codecClass : inputCodecClasses ) {
    		String lastExt = CodecManager.getCodecExtension(codecClass);
    		
    		if(lastExt != null ) {
    			extensions.add(lastExt);
    		}
    	}
    	
    	return extensions;
    }
    
    public Set<String> getAllOutputCodecExtensions() {
    	Set<String> extensions = new HashSet<String>();
    	
    	for(Class codecClass : outputCodecClasses ) {
    		String lastExt = CodecManager.getCodecExtension(codecClass);
    		
    		if(lastExt != null ) {
    			extensions.add(lastExt);
    		}
    	}
    	
    	return extensions;
    }

    /**
     * Returns the list of all registered output codecs.
     * 
     * @return the output codecs
     */
    public List<Class> getAllOutputCodecs() {
        return outputCodecClasses;
    }
    
    
    
    /**
     * Registers all codecs which are declared in a <code>codecclasses.properties</code>
     * file. This file is assumed to contain the names of codec
     * classes, one per line; all these classes will be registered
     * as codecs in this codec manager.<p> 
     * 
     * The method looks for the codecclasses file in the directory
     * <code>de/saar/chorus/domgraph/codec</code> below the classpath.
     * If there is more than one file with this pathname on the
     * class path, this method will process each of these files
     * in turn, i.e. the codecs of all files will be registered.
     * This is designed to make it easy to codec developers to write
     * their own codecs and use them from utool/domgraph without having
     * to recompile the domgraph code.
     * 
     * @throws CodecRegistrationException if an error occurred while trying
     * to register any of the listed codec classes. This happens when
     * either an I/O error occurred, one of the class names could not
     * be resolved to a class, or one of the classes is not a codec. 
     * 
     */
    public void registerAllDeclaredCodecs() throws CodecRegistrationException {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> urls = loader.getResources("de/saar/chorus/domgraph/codec/codecclasses.properties");

            while( urls.hasMoreElements() ) {
                URL url = urls.nextElement();
                InputStream is = url.openStream();

                Properties props = new Properties();
                props.load(is);
                
                for( Object name : props.keySet() ) {
                    registerCodec(Class.forName((String) name));
                }
            }
        } catch(Exception e) {
            throw new CodecRegistrationException("An error occurred while registering codecs from codecclass.properties", e);
        }
    }
    
    
    public List<File> getExampleFiles() {
    	List<File> ret = new ArrayList<File>();
    	File exampleFolder;
    	try{URI filelocation = 
    		Thread.currentThread().getContextClassLoader().
    		getResource("projects/Domgraph/examples/").toURI();
    	exampleFolder  = new File(filelocation);
    	} catch (Exception e) {
    		exampleFolder  = new File("projects/Domgraph/examples/");
    	}
    	 
    	if(! exampleFolder.isDirectory()) {
    		
    		try{URI filelocation = 
        		Thread.currentThread().getContextClassLoader().
        		getResource("examples/").toURI();
        	exampleFolder  = new File(filelocation);
        	} catch (Exception e) {
        		exampleFolder = new File("examples/");
        	}
    		
    		if(! exampleFolder.isDirectory()) {
			return ret;
    		}
    	}
    	
    	FileFilter exFilter = new InputCodecFilter();
		File[] exampleFiles = exampleFolder.listFiles(exFilter);
    	
		for( int i = 0; i< exampleFiles.length; i++ ) {
			exampleNameToPath.put(exampleFiles[i].getName(), 
					exampleFiles[i]);
			ret.add(exampleFiles[i]);
		}
    	
    	return ret;
    }
   
    
    private class InputCodecFilter implements FileFilter {

		/* (non-Javadoc)
		 * @see java.io.FileFilter#accept(java.io.File)
		 */
		public boolean accept(File pathname) {
			
		
				String name = pathname.getName();
				
				for(String ext : Ubench.getInstance().getCodecManager().getAllInputCodecExtensions() ) {
					if(name.endsWith(ext)) {
						return true;
					}
				}
				
			
			return false;
		}
		
	}
    
    public Reader getExampleReader(String exampleName) throws IOException {
    	
    	List<File> examples = getExampleFiles();
    	if(exampleNameToPath.containsKey(exampleName)){
    		InputStream exstream = new FileInputStream(exampleNameToPath.get(exampleName));
    		return new InputStreamReader(exstream);
    		
    	} else {
    		return null;
    	}
    } 

    /*
     * Unit tests:
     *  - exception when trying to register a class which isn't a codec
     *  - exception when trying to register a nameless codec (getName = null)
     */
    
    
}
