/*
 * @(#)CodecManager.java created 27.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;


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
    private List<OutputCodec> outputCodecs;
    private List<InputCodec> inputCodecs;
    
    public CodecManager() {
        outputCodecs = new ArrayList<OutputCodec>();
        inputCodecs = new ArrayList<InputCodec>();
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
    public void registerCodec(Class codecClass) throws CodecRegistrationException 
    {
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
    }
    
    /**
     * Gets an input codec object for the input codec with the
     * given name.
     * 
     * @param codecname the name of a registered input codec
     * @return an object of this codec class, or null if no codec
     * with this name was registered.
     */
    public InputCodec getInputCodecForName(String codecname) {
        for( InputCodec codec : inputCodecs ) {
            if( codecname.equals(codec.getName())) {
                return codec;
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
    public InputCodec getInputCodecForFilename(String filename) {
        for( InputCodec codec : inputCodecs ) {
            if( codec.getExtension() != null ) {
                if( filename.endsWith(codec.getExtension())) {
                    return codec;
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
    public OutputCodec getOutputCodecForName(String codecname) {
        for( OutputCodec codec : outputCodecs ) {
            if( codecname.equals(codec.getName())) {
                return codec;
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
    public OutputCodec getOutputCodecForFilename(String filename) {
        for( OutputCodec codec : outputCodecs ) {
            if( filename.endsWith(codec.getExtension())) {
                return codec;
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
        
        for( InputCodec codec : inputCodecs ) {
            max_strlen = Math.max(max_strlen, codec.getName().length());
        }

        for( OutputCodec codec : outputCodecs ) {
            max_strlen = Math.max(max_strlen, codec.getName().length());
        }
        
        formatString = "    %1$-" + max_strlen + "s             (%2$s)";
        formatStringNoExt = "    %1$s";
        

        
        out.println("Installed input codecs:");
        for( InputCodec codec : inputCodecs ) {
            if( codec.getExtension() == null ) {
                out.println(String.format(formatStringNoExt, codec.getName()));
            } else {
                out.println(String.format(formatString, codec.getName(), codec.getExtension()));
            }
        }
        
        out.println("\nInstalled output codecs:");
        for( OutputCodec codec : outputCodecs ) {
            if( codec.getExtension() == null ) {
                out.println(String.format(formatStringNoExt, codec.getName()));
            } else {
                out.println(String.format(formatString, codec.getName(), codec.getExtension()));
            }
        }
    }

    /**
     * Returns the list of all registered input codecs.
     * 
     * @return the input codecs
     */
    public List<InputCodec> getAllInputCodecs() {
        return inputCodecs;
    }

    /**
     * Returns the list of all registered output codecs.
     * 
     * @return the output codecs
     */
    public List<OutputCodec> getAllOutputCodecs() {
        return outputCodecs;
    }

}
