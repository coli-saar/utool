/*
 * @(#)CodecManager.java created 27.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.saar.chorus.ubench.gui.Ubench;

/**
 * A registry and factory for codecs. A codec manager is a place where different
 * codec classes can be registered, using the methods {@link #registerCodec(Class)}
 * or {@link #registerAllDeclaredCodecs()}. You can then ask the codec manager
 * to construct new codec objects for you, using the getInputCodecFor... and
 * getOutputCodecFor... methods.<p>
 * 
 * While you can always instantiate your own codec classes yourself, the recommended
 * way to dealing with codec classes is through a codec manager. This separates
 * the world of codecs from the rest of the system, offers a uniform interface
 * to all codecs, and deals with the codec metadata annotations correctly.
 * In other words, we encourage you to never instantiate a codec
 * class yourself, but use a codec manager for it.
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
    
    private Map<Class,Constructor> constructorForClass;
    
    private Map<String,File> exampleNameToPath;
    
    public CodecManager() {
        //outputCodecs = new ArrayList<OutputCodec>();
        //inputCodecs = new ArrayList<InputCodec>();
        outputCodecClasses = new ArrayList<Class>();
        inputCodecClasses = new ArrayList<Class>();
        constructorForClass = new HashMap<Class,Constructor>();
        exampleNameToPath = new HashMap<String,File>();
    }
    
    public static String getCodecName(Class codecClass) {
    	if( codecClass.isAnnotationPresent(CodecMetadata.class)) {
    		return ((CodecMetadata) codecClass.getAnnotation(CodecMetadata.class)).name();
    	} else {
    		return null;
    	}
    }
    
    public static String getCodecExtension(Class codecClass) {
    	if( codecClass.isAnnotationPresent(CodecMetadata.class)) {
    		String ret = ((CodecMetadata) codecClass.getAnnotation(CodecMetadata.class)).extension();
    		
    		if( "".equals(ret) ) {
    			return null;
    		} else {
    			return ret;
    		}
    	} else {
    		return null;
    	}
    }
    
    /**
     * Computes a map representing the parameter types of the constructor
     * of an input codec. The key of each entry is the name of the parameter,
     * as specified in its CodecOption annotation; its value is the class
     * of this parameter. The class may be any primitive type except for
     * <code>void</code> and <code>char</code>, the class <code>String</code>,
     * or any enumeration class. Use this method to query the user for a
     * string value for each parameter. You can then collect these values
     * in a map that assigns (String) values to parameter names, and use
     * this map to construct a new input codec object using the method
     * {@link #getInputCodecForName(String, Map)}. 
     * 
     * @param codecname the name of the input codec whose parameter
     * types you want
     * @return a map that assigns to each parameter name, the type
     * of this parameter
     */
    public Map<String,Class> getInputCodecOptionTypes(String codecname) {
        Class codecClass = getInputCodecClassForName(codecname);
        Map<String,Class> ret = new HashMap<String,Class>();
        Constructor con = constructorForClass.get(codecClass);
        Class[] parameterTypes = con.getParameterTypes();
        
        for( int i = 0; i < parameterTypes.length; i++ ) {
            ret.put(getParameterMetadata(con, i).name(), parameterTypes[i]);
        }
        
        return ret;
    }

    /**
     * Computes a map representing the parameter types of the constructor
     * of an output codec. The key of each entry is the name of the parameter,
     * as specified in its CodecOption annotation; its value is the class
     * of this parameter. The class may be any primitive type except for
     * <code>void</code> and <code>char</code>, the class <code>String</code>,
     * or any enumeration class. Use this method to query the user for a
     * string value for each parameter. You can then collect these values
     * in a map that assigns (String) values to parameter names, and use
     * this map to construct a new output codec object using the method
     * {@link #getOutputCodecForName(String, Map)}. 
     * 
     * @param codecname the name of the output codec whose parameter
     * types you want
     * @return a map that assigns to each parameter name, the type
     * of this parameter
     */
    public Map<String,Class> getOutputCodecOptionTypes(String codecname) {
        Class codecClass = getOutputCodecClassForName(codecname);
        Map<String,Class> ret = new HashMap<String,Class>();
        Constructor con = constructorForClass.get(codecClass);
        Class[] parameterTypes = con.getParameterTypes();
        
        for( int i = 0; i < parameterTypes.length; i++ ) {
            ret.put(getParameterMetadata(con, i).name(), parameterTypes[i]);
        }
        
        return ret;
    }

    /**
     * Finds the CodecOption annotation of a constructor parameter.
     * 
     * @param con the constructor of a codec class
     * @param index the position of the parameter among the parameters
     * of this constructor
     * @return the CodecOption annotation of the parameter, or null
     * if there is none
     */
    private CodecOption getParameterMetadata(Constructor con, int index) {
        for( int j = 0; j < con.getParameterAnnotations()[index].length; j++ ) {
            Annotation ann = con.getParameterAnnotations()[index][j];
            if( ann instanceof CodecOption ) {
                return (CodecOption) ann;
            }
        }
        
        return null;
    }
    
    private Object constructCodecWithOptions(Class codecClass, Map<String,String> optionMap) {
        Constructor con = constructorForClass.get(codecClass);
        Class[] parameterTypes = con.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];
        
        // iterate over the parameters of the constructor
        for( int i = 0; i < parameterTypes.length; i++ ) {
            CodecOption parameterData = getParameterMetadata(con, i);
            String valueAsString;
            
            // figure out value string
            valueAsString = parameterData.defaultValue();
            if( optionMap.containsKey(parameterData.name())) {
                valueAsString = optionMap.get(parameterData.name());
            }
            
            args[i] = stringToValue(valueAsString, parameterTypes[i]);
            if( args[i] == null ) {
                throw new UnsupportedOperationException("An error occurred while decoding the codec options: Value '" + valueAsString + "' couldn't be decoded as data type " + parameterTypes[i] + ".");
            }
        }
        
        // now use data to create new object
        try {
            return con.newInstance(args);
        } catch(InstantiationException e) {
            // should not happen: we checked at codec registration time
            // that class is not abstract
            assert false;
            return null;
        } catch (IllegalArgumentException e) {
            // should not happen: we constructed arguments of proper types
            assert false;
            return null;
        } catch (IllegalAccessException e) {
            // should not happen: we only looked at public constructors
            assert false;
            return null;
        } catch (InvocationTargetException e) {
            // we checked at registration time that constructor does not
            // throw checked exceptions, so if we get to this point,
            // we are looking at an Error or a RuntimeException
            if( e.getCause() instanceof Error ) {
                throw (Error) e.getCause();
            } else {
                throw (RuntimeException) e.getCause();
            }
        }
    }
    
    private Object constructCodecWithOptions(Class codecClass, String options) {
        return constructCodecWithOptions(codecClass, tokenizeOptions(options));
    }


    private boolean isValidParameterType(Class parameterType) {
        return (parameterType == String.class)
        || (parameterType.isPrimitive() && (parameterType != Void.TYPE) && (parameterType != Character.TYPE))
        || parameterType.isEnum();
    }


    private Object stringToValue(String valueAsString, Class asClass) {
        if( asClass == String.class ) {
            return valueAsString;
        } else if( asClass.isPrimitive() ) {
            if( asClass == Boolean.TYPE ) {
                return Boolean.valueOf(valueAsString);
            } else if( asClass == Byte.TYPE ) {
                return Byte.valueOf(valueAsString);
            } else if( asClass == Short.TYPE ) {
                return Short.valueOf(valueAsString);
            } else if( asClass == Integer.TYPE ) {
                return Integer.valueOf(valueAsString);
            } else if( asClass == Long.TYPE ) {
                return Long.valueOf(valueAsString);
            } else if( asClass == Float.TYPE ) {
                return Float.valueOf(valueAsString);
            } else if( asClass == Double.TYPE ) {
                return Double.valueOf(valueAsString);
            }
            // primitive types char, void are not supported 
        } else if( asClass.isEnum() ) {
            for( Object val : asClass.getEnumConstants() ) {
                if( val.toString().equals(valueAsString)) {
                    return val;
                }
            }
        }
        
        return null;
    }

    private Map<String, String> tokenizeOptions(String options) {
        Map<String,String> ret = new HashMap<String,String>();
        
        if( options != null ) {
            String regex = "\\s*=|,\\s*";
            String[] tokens = options.split(regex);
            int i = 0;
            
            while( i < tokens.length ) {
                ret.put(tokens[i], tokens[i+1]);
                i += 2;
            }
        }
            
        return ret;
    }

    
    
    
    /**
     * Registers a codec. Pass a codec class (not object) as the argument.
     * This class must be a subclass either of {@link InputCodec} or of
     * {@link OutputCodec}, and is filed under the input or output codecs
     * accordingly.<p>
     * 
     * Apart from the subclass requirement, a codec class must obey the
     * following rules:
     * <ul>
     * <li> It is a class that can be instantiated, i.e. not an abstract
     * class or an interface.
     * <li> It has a {@link CodecMetadata} annotation.
     * <li> It has at least one public constructor. If it has more than one
     * public constructor, then exactly one public constructor must have a
     * {@link CodecConstructor} annotation. We will the unique public constructor
     * or the unique annotated public constructor the "codec constructor" below.
     * <li> The codec constructor must not declare to throw any checked exceptions.
     * <li> All parameters of the codec constructor must have a {@link CodecOption}
     * annotation and must be of a primitive type (but not <code>void</code>
     * or <code>char</code>), an enumeration type, or the class <code>String</code>.
     * </ul>
     * 
     * @param codecClass a codec class
     * @throws CodecRegistrationException if the class violates any of the rules
     * specified above.
     */
    public void registerCodec(Class codecClass) throws CodecRegistrationException {
        // ensure that class can be instantiated
        if( (codecClass.getModifiers() & (Modifier.ABSTRACT | Modifier.INTERFACE)) > 0 ) {
            throw new CodecRegistrationException("Codec " + codecClass + " is not an instantiable type.");
        }
        
        // ensure that codec class has CodecMetadata annotation
        if( !codecClass.isAnnotationPresent(CodecMetadata.class)) {
            throw new CodecRegistrationException("Codec " + codecClass + " has no CodecMetadata annotation.");
        }

        // ensure that there is exactly one codec constructor
        Constructor[] constructors = codecClass.getConstructors(); 
        Constructor con = null;
        
        if( constructors.length == 1 ) {
            // if there is exactly one public constructor, it is the codec constructor
            con = constructors[0];
        } else {
            // otherwise, check whether there is a unique public constructor that
            // has a CodecConstructor annotation
            for( int i = 0; i < constructors.length; i++ ) {
                if( constructors[i].isAnnotationPresent(CodecConstructor.class)) {
                    if( con == null ) {
                        con = constructors[i];
                    } else {
                        // more than one public annotated constructor
                        throw new CodecRegistrationException("Codec " + codecClass + " declares more than one codec constructor.");
                    }
                }
            }
            
            if( con == null ) {
                // no public constructors
                throw new CodecRegistrationException("Codec " + codecClass + " doesn't declare a codec constructor.");
            }
        }
        
        // ensure that the constructor doesn't throw checked exceptions
        if( con.getExceptionTypes().length > 0 ) {
            throw new CodecRegistrationException("Codec " + codecClass + ": Constructor must not throw checked exceptions.");
        }
        
        // ensure that constructor parameters are annotated and have appropriate types
        for( int i = 0; i < con.getParameterTypes().length; i++ ) {
            if( getParameterMetadata(con, i) == null ) {
                throw new CodecRegistrationException("Codec " + codecClass + ": Constructor parameter " + i + " has no CodecOption annotation.");
            }
            
            if( !isValidParameterType(con.getParameterTypes()[i])) {
                throw new CodecRegistrationException("Codec " + codecClass + ": Constructor parameter " + getParameterMetadata(con, i).name() + " has inadmissible type " 
                        + con.getParameterTypes()[i] + ".");
            }
        }
        
        // finally, ensure that it is either a subclass of InputCodec or of
        // OutputCodec, and register it accordingly
        if( OutputCodec.class.isAssignableFrom(codecClass) ) {
            outputCodecClasses.add(codecClass);
            constructorForClass.put(codecClass, con);
        } else if( InputCodec.class.isAssignableFrom(codecClass)) {
            inputCodecClasses.add(codecClass);
            constructorForClass.put(codecClass, con);
        } else  {
            throw new CodecRegistrationException("Given codec " + codecClass + " is neither input nor output codec");
        }
    }
    
    private Class getInputCodecClassForName(String codecname) {
        for( Class codec : inputCodecClasses ) {
            if( codecname.equals(getCodecName(codec)) ) {
                return codec;
            }
        }
        
        return null;
    }
    
    private Class getOutputCodecClassForName(String codecname) {
        for( Class codec : outputCodecClasses ) {
            if( codecname.equals(getCodecName(codec)) ) {
                return codec;
            }
        }
        
        return null;
    }
    
    private Class getInputCodecClassForFilename(String filename) {
        for( Class codec : inputCodecClasses ) {
            String ext = getCodecExtension(codec);
            if( (ext != null) && filename.endsWith(ext) ) {
                return codec;
            }
        }
        
        return null;
    }
    
    private Class getOutputCodecClassForFilename(String filename) {
        for( Class codec : outputCodecClasses ) {
            String ext = getCodecExtension(codec);
            if( (ext != null) && filename.endsWith(ext) ) {
                return codec;
            }
        }
        
        return null;
    }
    
    
    /**
     * Constructs an input codec object for the input codec with the
     * given name.
     * 
     * @param codecname the name of a registered input codec
     * @param options an options string which is passed to the new codec 
     * @return an object of this codec class, or null if no codec
     * with this name was registered.
     */
    public InputCodec getInputCodecForName(String codecname, String options) {
        return (InputCodec) constructCodecWithOptions(getInputCodecClassForName(codecname), options);
    }
    
    /**
     * Constructs an input codec object for the input codec associated
     * with the given filename (extension).
     * 
     * @param filename the filename for which we need a codec
     * @param options an options string which is passed to the new codec 
     * @return an object of this codec class, or null if no codec
     * is associated with this filename extension
     */
    public InputCodec getInputCodecForFilename(String filename, String options) {
        return (InputCodec) constructCodecWithOptions(getInputCodecClassForFilename(filename), options);
    }
    
    /**
     * Constructs an output codec object for the output codec with the
     * given name.
     * 
     * @param codecname the name of a registered output codec
     * @param options an options string which is passed to the new codec 
     * @return an object of this codec class, or null if no codec
     * with this name was registered.
     */
    public OutputCodec getOutputCodecForName(String codecname, String options) {
        return (OutputCodec) constructCodecWithOptions(getOutputCodecClassForName(codecname), options);
    }
    
    /**
     * Constructs an output codec object for the output codec associated
     * with the given filename (extension).
     * 
     * @param filename the filename for which we need a codec
     * @param options an options string which is passed to the new codec 
     * @return an object of this codec class, or null if no codec
     * is associated with this filename extension
     */
    public OutputCodec getOutputCodecForFilename(String filename, String options) {
        return (OutputCodec) constructCodecWithOptions(getOutputCodecClassForFilename(filename), options);
    }

    
    /**
     * Constructs an input codec object for the input codec with the
     * given name.
     * 
     * @param codecname the name of a registered input codec
     * @param options a map that assigns values to parameter names of the codec class
     * @return an object of this codec class, or null if no codec
     * with this name was registered.
     */
    public InputCodec getInputCodecForName(String codecname, Map<String,String> options) {
        return (InputCodec) constructCodecWithOptions(getInputCodecClassForName(codecname), options);
    }
    
    /**
     * Constructs an input codec object for the input codec associated
     * with the given filename (extension).
     * 
     * @param filename the filename for which we need a codec
     * @param options a map that assigns values to parameter names of the codec class
     * @return an object of this codec class, or null if no codec
     * is associated with this filename extension
     */
    public InputCodec getInputCodecForFilename(String filename, Map<String,String> options) {
        return (InputCodec) constructCodecWithOptions(getInputCodecClassForFilename(filename), options);
    }
    
    /**
     * Constructs an output codec object for the output codec with the
     * given name.
     * 
     * @param codecname the name of a registered output codec
     * @param options a map that assigns values to parameter names of the codec class
     * @return an object of this codec class, or null if no codec
     * with this name was registered.
     */
    public OutputCodec getOutputCodecForName(String codecname, Map<String,String> options) {
        return (OutputCodec) constructCodecWithOptions(getOutputCodecClassForName(codecname), options);
    }
    
    /**
     * Constructs an output codec object for the output codec associated
     * with the given filename (extension).
     * 
     * @param filename the filename for which we need a codec
     * @param options a map that assigns values to parameter names of the codec class
     * @return an object of this codec class, or null if no codec
     * is associated with this filename extension
     */
    public OutputCodec getOutputCodecForFilename(String filename, Map<String,String> options) {
        return (OutputCodec) constructCodecWithOptions(getOutputCodecClassForFilename(filename), options);
    }

    
    /**
     * Prints an overview of all registered codecs to an output stream.
     * 
     * @param out the output stream to which the overview should be printed.
     */
    public void displayAllCodecs(PrintStream out) {
        int max_strlen = 0, max_extension_strlen = 0;
        String formatString;
        
        List<String> inputCodecNames = new ArrayList<String>();
        List<String> outputCodecNames = new ArrayList<String>();
        Map<String,Class> inputCodecClassMap = new HashMap<String,Class>();
        Map<String,Class> outputCodecClassMap = new HashMap<String,Class>();
        
        for( Class codec : inputCodecClasses ) {
            max_strlen = Math.max(max_strlen, getCodecName(codec).length());
            if( getCodecExtension(codec) != null ) {
                max_extension_strlen = Math.max(max_extension_strlen, getCodecExtension(codec).length());
            }
            
            inputCodecNames.add(getCodecName(codec));
            inputCodecClassMap.put(getCodecName(codec), codec);
        }

        for( Class codec : outputCodecClasses ) {
            max_strlen = Math.max(max_strlen, getCodecName(codec).length());
            if( getCodecExtension(codec) != null ) {
                max_extension_strlen = Math.max(max_extension_strlen, getCodecExtension(codec).length());
            }

            outputCodecNames.add(getCodecName(codec));
            outputCodecClassMap.put(getCodecName(codec), codec);
        }
        
        formatString = "    %1$-" + max_strlen + "s             %2$-" + (max_extension_strlen+2) + "s%3$s";
        
        
        Collections.sort(inputCodecNames);
        Collections.sort(outputCodecNames);
        
        
        out.println("Installed input codecs:");
        for( String inputCodecName : inputCodecNames ) {
            displayOneCodec(inputCodecClassMap.get(inputCodecName), formatString, out);
        }
        
        out.println("\nInstalled output codecs:");
        for( String outputCodecName : outputCodecNames ) {
            displayOneCodec(outputCodecClassMap.get(outputCodecName), formatString, out);
        }
    }

    private void displayOneCodec(Class codec, String formatString, PrintStream out) {
        String name = getCodecName(codec);
        String ext = getCodecExtension(codec);
        String experimentalString = getCodecAnnotation(codec).experimental() ? " (EXPERIMENTAL!)" : "";
        
        if( ext == null ) {
            out.println(String.format(formatString, name, "", experimentalString));
        } else {
            out.println(String.format(formatString, name, "(" + ext + ")", experimentalString));
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
    
    public List<String> getAllInputCodecExtensions() {
    	List<String> extensions = new ArrayList<String>();
    	
    	for(Class codecClass : inputCodecClasses ) {
    		String lastExt = CodecManager.getCodecExtension(codecClass);
    		
    		if(lastExt != null ) {
    			extensions.add(lastExt);
    		}
    	}
    	
    	return extensions;
    }
    
    public List<String> getAllOutputCodecExtensions() {
    	List<String> extensions = new ArrayList<String>();
    	
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
     * to recompile the domgraph code.<p>
     * 
     * Each codec that is registered using this method is still subject
     * to the rules laid out in the documentation of {@link #registerCodec(Class)}.
     * 
     * @throws CodecRegistrationException if an error occurred while trying
     * to register any of the listed codec classes. This happens when
     * either an I/O error occurred, one of the class names could not
     * be resolved to a class, or one of the classes is not a valid codec. 
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
    
    /*
    public Reader getExampleReader(String exampleName) throws IOException {
    	
    	List<File> examples = getExampleFiles();
    	if(exampleNameToPath.containsKey(exampleName)){
    		InputStream exstream = new FileInputStream(exampleNameToPath.get(exampleName));
    		return new InputStreamReader(exstream);
    		
    	} else {
    		return null;
    	}
    } 
    */
    
    private CodecMetadata getCodecAnnotation(Class codecClass) {
        return (CodecMetadata) codecClass.getAnnotation(CodecMetadata.class);
    }
    
    
    

    /*
     * Unit tests:
     *  - exception when trying to register a class which isn't a codec
     *  - exception when trying to register a nameless codec (getName = null)
     */
    
    
}

