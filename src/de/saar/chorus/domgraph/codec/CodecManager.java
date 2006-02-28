/*
 * @(#)CodecManager.java created 27.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;


/**
 * A registry for codecs. Objects of this class are intended as a
 * place where different codec classes can be registered. Then
 * it is possible to look up codecs via their names or their
 * filename extensions.
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
    
    
    public CodecManager() {
        //outputCodecs = new ArrayList<OutputCodec>();
        //inputCodecs = new ArrayList<InputCodec>();
        outputCodecClasses = new ArrayList<Class>();
        inputCodecClasses = new ArrayList<Class>();
    }
    
    public void registerAllVisibleCodecs() {
        List<String> classnames = new ArrayList<String>();
        Package[] pcks = Package.getPackages();
        
        for (int i = 0; i < pcks.length; i++) {
            // normalise package name
            String name = pcks[i].getName();
            if (!name.startsWith("/")) {
                name = "/" + name;
            }
            name = name.replace('.','/');
            
            // Get a File object for the package
            URL url = CodecManager.class.getResource(name);
            
            if( url != null ) {
                // TODO replace %... entities in a more general way
                String urlname = url.getFile().replaceAll("%20", " ");
                File directory = new File(urlname);
                
                if (directory.exists()) {
                    // URL describes an existing package directory
                    String[] files = directory.list();
                    
                    for (int j = 0; j < files.length; j++) {
                        if (files[j].endsWith(".class")) {
                            classnames.add(pcks[i].getName() + "." + files[j].substring(0,files[j].length()-6).replaceAll("/", "."));
                        }
                    }
                } else {
                    // URL describes an entry in a JAR file
                    try {
                        JarURLConnection conn = (JarURLConnection) url.openConnection();
                        String starts = conn.getEntryName();
                        JarFile jfile = conn.getJarFile();
                        Enumeration e = jfile.entries();
                        
                        while (e.hasMoreElements()) {
                            ZipEntry entry = (ZipEntry) e.nextElement();
                            String entryname = entry.getName();
                            
                            if (entryname.startsWith(starts)
                                    && (entryname.lastIndexOf('/')<=starts.length())
                                    && entryname.endsWith(".class")) {
                                classnames.add(entryname.substring(0,entryname.length()-6).replaceAll("/", "."));
                            }
                        }
                    } catch(IOException e) {
                        // i.e. had problems reading from the JAR file
                        // -- just ignore this
                        System.err.println(e);
                    }
                }
            }   
        }
        
        // go through class names and register the codec classes
        for( String classname : classnames ) {
            try {
                Class c = Class.forName(classname);
                
                //System.err.println("\nclass: " + c);
                if( InputCodec.class.isAssignableFrom(c) || OutputCodec.class.isAssignableFrom(c)) {
                  //  System.err.println("try to register " + c);
                    registerCodec(c);
                   // System.err.println("  - success!");
                }
            } catch(ClassNotFoundException e) {
                // i.e. couldn't reconstruct a valid class name from
                // the .class filename -- just ignore it
            } catch (CodecRegistrationException e) {
                // i.e. the class was not a valid codec class after all
                // (in particular because it was abstract) -- just ignore it
                //System.err.println("  - but caught CRE");
            }
        }
        
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
    
    private InputCodec constructInputCodec(Class codecClass, String options)  {
        Object ret = null;
        
        try {
            Constructor con = codecClass.getConstructor(String.class);
            ret = con.newInstance(options);
        } catch(Exception e) {
            // couldn't call the Constructor(String).
            try {
                ret = codecClass.newInstance();
            } catch(Exception f) {
            }
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
        } catch(Exception e) {
            // couldn't call the Constructor(String).
            try {
                Constructor con = codecClass.getConstructor();
                ret = con.newInstance();
            } catch(Exception f) {
           }
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
     * of <code>InputCodec</code> nor of <code>OutputCodec</code>.
     */
    public void registerCodec(Class codecClass) throws CodecRegistrationException {
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
        
        
/*         
        try {
            if( OutputCodec.class.isAssignableFrom(codecClass) ) {
                Constructor constr;
                constr = codecClass.getConstructor(new Class[] { });
                OutputCodec codec = (OutputCodec) constr.newInstance(new Object[] { });
                outputCodecs.add(codec);
            } else if( InputCodec.class.isAssignableFrom(codecClass)) {
                Constructor constr;
                constr = codecClass.getConstructor(new Class[] { });
                InputCodec codec = (InputCodec) constr.newInstance(new Object[] { });
                inputCodecs.add(codec);
            }
        } catch (Exception e) {
            throw new CodecRegistrationException(e);
        }
        */ 
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
        
        for( Class codec : inputCodecClasses ) {
            max_strlen = Math.max(max_strlen, getCodecName(codec).length());
        }

        for( Class codec : outputCodecClasses ) {
            max_strlen = Math.max(max_strlen, getCodecName(codec).length());
        }
        
        formatString = "    %1$-" + max_strlen + "s             (%2$s)";
        formatStringNoExt = "    %1$s";
        

        
        out.println("Installed input codecs:");
        for( Class codec : inputCodecClasses ) {
            displayOneCodec(codec, formatString, formatStringNoExt, out);
        }
        
        out.println("\nInstalled output codecs:");
        for( Class codec : outputCodecClasses ) {
            displayOneCodec(codec, formatString, formatStringNoExt, out);
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

    /**
     * Returns the list of all registered output codecs.
     * 
     * @return the output codecs
     */
    public List<Class> getAllOutputCodecs() {
        return outputCodecClasses;
    }

}
